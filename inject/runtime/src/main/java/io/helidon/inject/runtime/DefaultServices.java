/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.inject.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.ActivationRequest;
import io.helidon.inject.api.Activator;
import io.helidon.inject.api.Application;
import io.helidon.inject.api.CallingContext;
import io.helidon.inject.api.CallingContextFactory;
import io.helidon.inject.api.InjectTypes;
import io.helidon.inject.api.InjectionException;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.InjectionServicesConfig;
import io.helidon.inject.api.Metrics;
import io.helidon.inject.api.ModuleComponent;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.Qualifier;
import io.helidon.inject.api.Resettable;
import io.helidon.inject.api.ServiceBinder;
import io.helidon.inject.api.ServiceDescriptor;
import io.helidon.inject.api.ServiceInfoCriteria;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceProviderInjectionException;
import io.helidon.inject.api.ServiceProviderProvider;
import io.helidon.inject.api.ServiceSource;
import io.helidon.inject.api.Services;

import jakarta.inject.Provider;

/**
 * The default reference implementation of {@link Services}.
 */
class DefaultServices implements Services, ServiceBinder, Resettable {
    private static final ServiceProviderComparator COMPARATOR = ServiceProviderComparator.create();

    private final ConcurrentHashMap<TypeName, ServiceProvider<?>> servicesByTypeName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TypeName, Set<ServiceProvider<?>>> servicesByContract = new ConcurrentHashMap<>();
    private final Map<ServiceInfoCriteria, List<ServiceProvider<?>>> cache = new ConcurrentHashMap<>();
    private final InjectionServices injectionServices;
    private final InjectionServicesConfig cfg;
    private final AtomicInteger lookupCount = new AtomicInteger();
    private final AtomicInteger cacheLookupCount = new AtomicInteger();
    private final AtomicInteger cacheHitCount = new AtomicInteger();
    // a ma of service provider instances to their activators, so we can correctly handle activation requests
    private final Map<ServiceProvider<?>, Activator<?>> providersToActivators = new IdentityHashMap<>();
    private volatile State stateWatchOnly; // we are watching and not mutating this state - owned by DefaultInjectionServices

    /**
     * The constructor taking a configuration.
     *
     * @param injectionServices injection services
     * @param cfg               the config
     */
    DefaultServices(InjectionServices injectionServices, InjectionServicesConfig cfg) {
        this.injectionServices = injectionServices;
        this.cfg = Objects.requireNonNull(cfg);
    }

    static List<ServiceProvider<?>> explodeFilterAndSort(final Collection<ServiceProvider<?>> coll,
                                                         ServiceInfoCriteria criteria,
                                                         boolean expected) {
        List<ServiceProvider<?>> exploded;
        if ((coll.size() > 1)
                || coll.stream().anyMatch(sp -> sp instanceof ServiceProviderProvider)) {
            exploded = new ArrayList<>();

            coll.forEach(s -> {
                if (s instanceof ServiceProviderProvider spp) {
                    List<? extends ServiceProvider<?>> subList = spp.serviceProviders(criteria, true, true);
                    if (subList != null && !subList.isEmpty()) {
                        subList.stream().filter(Objects::nonNull).forEach(exploded::add);
                    }
                } else {
                    exploded.add(s);
                }
            });
        } else {
            exploded = new ArrayList<>(coll);
        }

        if (expected && exploded.isEmpty()) {
            throw resolutionBasedInjectionError(criteria);
        }

        if (exploded.size() > 1) {
            exploded.sort(serviceProviderComparator());
        }

        // the providers are sorted by weight and other properties
        // we need to have unnamed providers before named ones (if criteria does not contain a Named qualifier)
        // in similar fashion, if criteria does not contain any qualifier, put unqualified instances first
        if (criteria.qualifiers().isEmpty()) {
            // unqualified first, unnamed before named, but keep the existing order otherwise
            List<ServiceProvider<?>> unqualified = new ArrayList<>();
            List<ServiceProvider<?>> qualified = new ArrayList<>();
            for (ServiceProvider<?> serviceProvider : exploded) {
                if (serviceProvider.qualifiers().isEmpty()) {
                    unqualified.add(serviceProvider);
                } else {
                    qualified.add(serviceProvider);
                }
            }
            unqualified.addAll(qualified);
            return unqualified;
        } else if (!hasNamed(criteria.qualifiers())) {
            // unnamed first
            List<ServiceProvider<?>> unnamed = new ArrayList<>();
            List<ServiceProvider<?>> named = new ArrayList<>();
            for (ServiceProvider<?> serviceProvider : exploded) {
                if (hasNamed(serviceProvider.qualifiers())) {
                    named.add(serviceProvider);
                } else {
                    unnamed.add(serviceProvider);
                }
            }
            unnamed.addAll(named);
            return unnamed;
        }

        return exploded;
    }

    private static boolean hasNamed(Set<Qualifier> qualifiers) {
        return qualifiers.stream()
                .anyMatch(it -> it.typeName().equals(InjectTypes.JAKARTA_NAMED));
    }

    /**
     * First use weight, then use FQN of the service type name as the secondary comparator if weights are the same.
     *
     * @return the comparator
     * @see ServiceProviderComparator
     */
    static Comparator<? super Provider<?>> serviceProviderComparator() {
        return COMPARATOR;
    }

    static void assertPermitsDynamic(InjectionServicesConfig cfg) {
        if (!cfg.permitsDynamic()) {
            String desc = "Services are configured to prevent dynamic updates.\n"
                    + "Set config 'inject.permits-dynamic = true' to enable";
            Optional<CallingContext> callCtx = CallingContextFactory.create(false);
            String msg = callCtx.map(callingContext -> InjectionExceptions.toErrorMessage(callingContext, desc))
                    .orElseGet(() -> InjectionExceptions.toErrorMessage(desc));
            throw new IllegalStateException(msg);
        }
    }

    static ServiceProviderInjectionException serviceProviderAlreadyBoundInjectionError(ServiceProvider<?> previous,
                                                                                       ServiceProvider<?> sp) {
        return new ServiceProviderInjectionException("Service provider already bound to " + previous, null, sp);
    }

    static ServiceProviderInjectionException resolutionBasedInjectionError(ServiceInfoCriteria ctx) {
        return new ServiceProviderInjectionException("Expected to resolve a service matching " + ctx);
    }

    static ServiceProviderInjectionException resolutionBasedInjectionError(TypeName serviceTypeName) {
        return resolutionBasedInjectionError(ServiceInfoCriteria.builder().serviceTypeName(serviceTypeName).build());
    }

    /**
     * Total size of the service registry.
     *
     * @return total size of the service registry
     */
    public int size() {
        return servicesByTypeName.size();
    }

    /**
     * Performs a reset. When deep is false this will only clear the cache and metrics count. When deep is true will also
     * deeply reset each service in the registry as well as clear out the registry. Dynamic must be permitted in config for
     * reset to occur.
     *
     * @param deep set to true will iterate through every service in the registry to attempt a reset on each service as well
     * @return true if reset had any affect
     * @throws java.lang.IllegalStateException when dynamic is not permitted
     */
    @Override
    public boolean reset(boolean deep) {
        if (Phase.ACTIVATION_STARTING != currentPhase()) {
            assertPermitsDynamic(cfg);
        }

        boolean changed = (deep || !servicesByTypeName.isEmpty() || lookupCount.get() > 0 || cacheLookupCount.get() > 0);

        if (deep) {
            servicesByTypeName.values().forEach(sp -> {
                if (sp instanceof Resettable) {
                    ((Resettable) sp).reset(true);
                }
            });
            servicesByTypeName.clear();
            servicesByContract.clear();
        }

        clearCacheAndMetrics();

        return changed;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Optional<ServiceProvider<T>> lookupFirst(Class<T> type,
                                                        boolean expected) {
        ServiceInfoCriteria criteria = ServiceInfoCriteria.builder()
                .addContract(TypeName.create(type))
                .build();
        return (Optional) lookupFirst(criteria, expected);
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Optional<ServiceProvider<T>> lookupFirst(Class<T> type,
                                                        String name,
                                                        boolean expected) {
        ServiceInfoCriteria criteria = ServiceInfoCriteria.builder()
                .addContract(TypeName.create(type))
                .addQualifier(Qualifier.createNamed(name))
                .build();
        return (Optional) lookupFirst(criteria, expected);
    }

    @Override
    public Optional<ServiceProvider<?>> lookupFirst(ServiceInfoCriteria criteria,
                                                    boolean expected) {
        List<ServiceProvider<?>> result = lookup(criteria, expected, 1);
        assert (!expected || !result.isEmpty());
        return (result.isEmpty()) ? Optional.empty() : Optional.of(result.getFirst());
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> List<ServiceProvider<T>> lookupAll(Class<T> type) {
        ServiceInfoCriteria serviceInfo = ServiceInfoCriteria.builder()
                .addContract(TypeName.create(type))
                .build();
        return (List) lookup(serviceInfo, false, Integer.MAX_VALUE);
    }

    @Override
    public List<ServiceProvider<?>> lookupAll(ServiceInfoCriteria criteria,
                                              boolean expected) {
        List<ServiceProvider<?>> result = lookup(criteria, expected, Integer.MAX_VALUE);
        assert (!expected || !result.isEmpty());
        return result;
    }

    @Override
    public void bind(ServiceSource<?> serviceDescriptor) {
        bind(ServiceBinderDefault.serviceActivator(this.injectionServices, serviceDescriptor));
    }

    @Override
    public void bind(Activator<?> serviceActivator) {
        if (currentPhase().ordinal() > Phase.GATHERING_DEPENDENCIES.ordinal()) {
            assertPermitsDynamic(cfg);
        }

        // make sure the activator has a chance to do something, such as create the initial service provider instance
        serviceActivator.activate(ActivationRequest.builder()
                                          .targetPhase(Phase.INIT)
                                          .throwIfError(false)
                                          .build());

        ServiceProvider<?> serviceProvider = serviceActivator.serviceProvider();
        this.providersToActivators.put(serviceProvider, serviceActivator);

        TypeName serviceTypeName = serviceProvider.serviceType();

        ServiceProvider<?> previous = servicesByTypeName.putIfAbsent(serviceTypeName, serviceProvider);
        if (previous != null && previous != serviceProvider) {
            if (cfg.permitsDynamic()) {
                DefaultInjectionServices.LOGGER.log(System.Logger.Level.WARNING,
                                                    "overwriting " + previous + " with " + serviceProvider);
                servicesByTypeName.put(serviceTypeName, serviceProvider);
                providersToActivators.remove(previous);
            } else {
                providersToActivators.remove(previous);
                throw serviceProviderAlreadyBoundInjectionError(previous, serviceProvider);
            }
        }

        boolean added = servicesByContract.computeIfAbsent(serviceTypeName, it -> new TreeSet<>(serviceProviderComparator()))
                .add(serviceProvider);
        assert (added) : "expected to have added: " + serviceProvider;

        for (TypeName cn : serviceProvider.contracts()) {
            servicesByContract.compute(cn, (contract, servicesSharingThisContract) -> {
                if (servicesSharingThisContract == null) {
                    servicesSharingThisContract = new TreeSet<>(serviceProviderComparator());
                }
                boolean ignored = servicesSharingThisContract.add(serviceProvider);
                return servicesSharingThisContract;
            });
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ServiceProvider<T> serviceProvider(ServiceDescriptor<T> descriptor) {
        return (ServiceProvider<T>) servicesByTypeName.get(descriptor.serviceType());
    }

    Optional<Activator<?>> activator(ServiceProvider<?> instance) {
        return Optional.ofNullable(providersToActivators.get(instance));
    }

    void state(State state) {
        this.stateWatchOnly = Objects.requireNonNull(state);
    }

    Phase currentPhase() {
        return (stateWatchOnly == null) ? Phase.INIT : stateWatchOnly.currentPhase();
    }

    Map<ServiceInfoCriteria, List<ServiceProvider<?>>> cache() {
        return Map.copyOf(cache);
    }

    /**
     * Clear the cache and metrics.
     */
    void clearCacheAndMetrics() {
        cache.clear();
        lookupCount.set(0);
        cacheLookupCount.set(0);
        cacheHitCount.set(0);
    }

    Metrics metrics() {
        return Metrics.builder()
                .serviceCount(size())
                .lookupCount(lookupCount.get())
                .cacheLookupCount(cacheLookupCount.get())
                .cacheHitCount(cacheHitCount.get())
                .build();
    }

    @SuppressWarnings({"rawtypes"})
    List<ServiceProvider<?>> lookup(ServiceInfoCriteria criteria,
                                    boolean expected,
                                    int limit) {

        List<ServiceProvider<?>> result;

        lookupCount.incrementAndGet();

        if (criteria.serviceTypeName().isPresent()) {
            // when a specific service type is requested, we go for it
            ServiceProvider exact = servicesByTypeName.get(criteria.serviceTypeName().get());
            if (exact != null) {
                return explodeFilterAndSort(List.of(exact), criteria, expected);
            }
        }

        if (1 == criteria.contracts().size()) {
            TypeName theOnlyContractRequested = criteria.contracts().iterator().next();
            Set<ServiceProvider<?>> subsetOfMatches = servicesByContract.get(theOnlyContractRequested);
            if (subsetOfMatches != null) {
                result = subsetOfMatches.stream()
                        .parallel()
                        .filter(criteria::matches)
                        .limit(limit)
                        .toList();
                if (!result.isEmpty()) {
                    return explodeFilterAndSort(result, criteria, expected);
                }
            }
        }

        if (cfg.serviceLookupCaching()) {
            result = cache.get(criteria);
            cacheLookupCount.incrementAndGet();
            if (result != null) {
                cacheHitCount.incrementAndGet();
                return result;
            }
        }

        // table scan :-(
        result = servicesByTypeName.values()
                .stream().parallel()
                .filter(criteria::matches)
                .limit(limit)
                .toList();
        if (expected && result.isEmpty()) {
            throw resolutionBasedInjectionError(criteria);
        }

        if (!result.isEmpty()) {
            result = explodeFilterAndSort(result, criteria, expected);
        }

        if (cfg.serviceLookupCaching()) {
            cache.put(criteria, List.copyOf(result));
        }

        return result;
    }

    ServiceProvider<?> serviceProviderFor(TypeName serviceTypeName) {
        ServiceProvider<?> serviceProvider = servicesByTypeName.get(serviceTypeName);
        if (serviceProvider == null) {
            throw resolutionBasedInjectionError(serviceTypeName);
        }
        return serviceProvider;
    }

    List<ServiceProvider<?>> allServiceProviders(boolean explode) {
        if (explode) {
            return explodeFilterAndSort(servicesByTypeName.values(), InjectionServices.EMPTY_CRITERIA, false);
        }

        return new ArrayList<>(servicesByTypeName.values());
    }

    ServiceBinder createServiceBinder(InjectionServices injectionServices,
                                      DefaultServices services,
                                      String moduleName,
                                      boolean trusted) {
        assert (injectionServices.services() == services);
        return ServiceBinderDefault.create(injectionServices, moduleName, trusted);
    }

    void bind(InjectionServices injectionServices,
              DefaultInjectionPlanBinder binder,
              Application app) {
        String appName = app.name();
        boolean isLoggable = DefaultInjectionServices.LOGGER.isLoggable(System.Logger.Level.DEBUG);
        if (isLoggable) {
            DefaultInjectionServices.LOGGER.log(System.Logger.Level.DEBUG, "Starting binding application: " + appName);
        }
        try {
            app.configure(binder);
            bind(createServiceActivator(app, injectionServices));
            if (isLoggable) {
                DefaultInjectionServices.LOGGER.log(System.Logger.Level.DEBUG, "Finished binding application: " + appName);
            }
        } catch (Exception e) {
            throw new InjectionException("Failed to process: " + app, e);
        }
    }

    void bind(InjectionServices injectionServices,
              ModuleComponent module,
              boolean initializing) {
        String moduleName = module.name();
        boolean isLoggable = DefaultInjectionServices.LOGGER.isLoggable(System.Logger.Level.TRACE);
        if (isLoggable) {
            DefaultInjectionServices.LOGGER.log(System.Logger.Level.TRACE, "starting binding module: " + moduleName);
        }
        ServiceBinder moduleServiceBinder = createServiceBinder(injectionServices, this, moduleName, initializing);
        module.configure(moduleServiceBinder);
        bind(createServiceActivator(module, moduleName, injectionServices));
        if (isLoggable) {
            DefaultInjectionServices.LOGGER.log(System.Logger.Level.TRACE, "finished binding module: " + moduleName);
        }
    }

    private Activator<?> createServiceActivator(ModuleComponent module,
                                                String moduleName,
                                                InjectionServices injectionServices) {
        return InjectionModuleActivator.create(injectionServices, module, moduleName);
    }

    private Activator<?> createServiceActivator(Application app,
                                                InjectionServices injectionServices) {
        return InjectionApplicationActivator.create(injectionServices, app);
    }
}
