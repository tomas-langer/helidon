package io.helidon.inject;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.helidon.common.HelidonServiceLoader;
import io.helidon.common.types.TypeName;
import io.helidon.inject.service.Interception;
import io.helidon.inject.service.Ip;
import io.helidon.inject.service.ModuleComponent;
import io.helidon.inject.service.Qualifier;
import io.helidon.inject.service.ServiceBinder;
import io.helidon.inject.service.ServiceDescriptor;
import io.helidon.inject.service.ServiceInfo;
import io.helidon.inject.spi.ActivatorProvider;
import io.helidon.metrics.api.Counter;
import io.helidon.metrics.api.Meter;
import io.helidon.metrics.api.Metrics;

class ServicesImpl implements Services, ServiceBinder {
    private static final System.Logger LOGGER = System.getLogger(Services.class.getName());
    private static final Map<String, ActivatorProvider> ACTIVATOR_PROVIDERS;

    static {
        Map<String, ActivatorProvider> activators = new HashMap<>();

        HelidonServiceLoader.builder(ServiceLoader.load(ActivatorProvider.class))
                .addService(new InjectActivatorProvider())
                .build()
                .asList()
                .forEach(it -> activators.putIfAbsent(it.id(), it));
        ACTIVATOR_PROVIDERS = Map.copyOf(activators);
    }

    private final Map<Lookup, List<ServiceProvider<?>>> cache = new ConcurrentHashMap<>();
    // a map of service provider instances to their activators, so we can correctly handle activation requests
    private final Map<ServiceProvider<?>, Activator<?>> providersToActivators = new IdentityHashMap<>();
    private final Counter lookupCounter;
    private final InjectionConfig cfg;
    private final Map<TypeName, ServiceProvider<?>> servicesByTypeName;
    private final Map<TypeName, Set<ServiceProvider<?>>> servicesByContract;
    private final Counter cacheLookupCounter;
    private final Counter cacheHitCounter;
    private final InjectionServicesImpl injectionServices;
    private final State state;

    private volatile List<ServiceProvider<Interception.Interceptor>> interceptors;

    ServicesImpl(InjectionServicesImpl injectionServices, State state) {
        this.injectionServices = injectionServices;
        this.state = state;
        this.cfg = injectionServices.config();

        this.lookupCounter = Metrics.globalRegistry()
                .getOrCreate(Counter.builder("io.helidon.inject.lookups")
                                     .description("Number of lookups in the service registry")
                                     .scope(Meter.Scope.VENDOR));
        if (cfg.serviceLookupCaching()) {
            this.cacheLookupCounter = Metrics.globalRegistry()
                    .getOrCreate(Counter.builder("io.helidon.inject.cacheLookups")
                                         .description("Number of lookups in cache in the service registry")
                                         .scope(Meter.Scope.VENDOR));
            this.cacheHitCounter = Metrics.globalRegistry()
                    .getOrCreate(Counter.builder("io.helidon.inject.cacheHits")
                                         .description("Number of cache hits in the service registry")
                                         .scope(Meter.Scope.VENDOR));
        } else {
            this.cacheLookupCounter = null;
            this.cacheHitCounter = null;
        }

        this.servicesByTypeName = new ConcurrentHashMap<>();
        this.servicesByContract = new ConcurrentHashMap<>();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<Supplier<T>> find(Lookup criteria) {
        return this.<T>lookup(criteria, 1)
                .stream()
                .findFirst();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<Supplier<T>> all(Lookup criteria) {
        return this.lookup(criteria, Integer.MAX_VALUE);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<ServiceProvider<T>> serviceProviders(Lookup criteria) {
        return this.lookup(criteria, Integer.MAX_VALUE);
    }

    @Override
    public void bind(ServiceDescriptor<?> serviceDescriptor) {
        ActivatorProvider activatorProvider = ACTIVATOR_PROVIDERS.get(serviceDescriptor.runtimeId());
        if (activatorProvider == null) {
            throw new IllegalStateException("Expected an activator provider for runtime id: " + serviceDescriptor.runtimeId()
                                                    + ", available activator providers: " + ACTIVATOR_PROVIDERS.keySet());
        }
        bind(activatorProvider.activator(this, serviceDescriptor));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ServiceProvider<T> serviceProvider(ServiceInfo serviceInfo) {
        ServiceProvider<?> serviceProvider = servicesByTypeName.get(serviceInfo.serviceType());
        if (serviceProvider == null) {
            throw new NoSuchElementException("Requested service is not managed by this registry: "
                                                     + serviceInfo.serviceType().fqName());
        }
        return (ServiceProvider<T>) serviceProvider;
    }

    @Override
    public InjectionServicesImpl injectionServices() {
        return injectionServices;
    }

    @Override
    public ServiceBinder binder() {
        return this;
    }

    void bindSelf() {
        bind(ServicesActivator.create(this));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    void interceptors(ServiceInfo... serviceInfos) {
        if (this.interceptors == null) {
            List list = Stream.of(serviceInfos)
                    .map(this::serviceProvider)
                    .toList();
            this.interceptors = List.copyOf(list);
        }
    }

    List<ServiceProvider<Interception.Interceptor>> interceptors() {
        if (interceptors == null) {
            interceptors = serviceProviders(Lookup.builder()
                                                    .addContract(Interception.Interceptor.class)
                                                    .addQualifier(Qualifier.WILDCARD_NAMED)
                                                    .build());
        }
        return interceptors;
    }

    void bind(ModuleComponent module) {
        String moduleName = module.name();

        if (LOGGER.isLoggable(Level.TRACE)) {
            LOGGER.log(Level.TRACE, "Starting module binding: " + moduleName);
        }

        ServiceBinder moduleBinder = new ServiceBinderImpl(moduleName);
        module.configure(moduleBinder);
        bind(InjectionModuleActivator.create(this, module, moduleName));

        if (LOGGER.isLoggable(Level.TRACE)) {
            LOGGER.log(Level.TRACE, "Finished module binding: " + moduleName);

        }
    }

    void bind(Application application) {
        String appName = application.name();

        if (LOGGER.isLoggable(Level.TRACE)) {
            LOGGER.log(Level.TRACE, "Starting application binding: " + appName);
        }

        ServiceInjectionPlanBinder appBinder = new AppBinderImpl(appName);
        application.configure(appBinder);
        bind(InjectionApplicationActivator.create(this, application, appName));

        if (LOGGER.isLoggable(Level.TRACE)) {
            LOGGER.log(Level.TRACE, "Finished application binding: " + appName);

        }
    }

    Optional<Activator<?>> activator(ServiceProvider<?> instance) {
        return Optional.ofNullable(providersToActivators.get(instance));
    }

    List<ServiceProvider<?>> allProviders() {
        Set<ServiceProvider<?>> result = new HashSet<>(servicesByTypeName.values());
        servicesByContract.values()
                .forEach(result::addAll);

        return result.stream()
                .toList();
    }

    private static boolean hasNamed(Set<Qualifier> qualifiers) {
        return qualifiers.stream()
                .anyMatch(it -> it.typeName().equals(InjectTypes.NAMED));
    }

    private void bind(Activator<?> activator) {
        if (state.currentPhase().ordinal() > Phase.GATHERING_DEPENDENCIES.ordinal()) {
            if (!cfg.permitsDynamic()) {
                throw new IllegalStateException(
                        "Attempting to bind to Services that do not support dynamic updates. Set option permitsDynamic, "
                                + "or configuration option 'inject.permits-dynamic=true' to enable");
            }
        }

        // make sure the activator has a chance to do something, such as create the initial service provider instance
        activator.activate(ActivationRequest.builder()
                                   .targetPhase(Phase.INIT)
                                   .throwIfError(false)
                                   .build());
        ServiceProvider<?> serviceProvider = activator.serviceProvider();
        this.providersToActivators.put(serviceProvider, activator);

        TypeName serviceType = serviceProvider.serviceType();

        // only put if absent, as this may be a lower weight provider for the same type
        servicesByTypeName.putIfAbsent(serviceType, serviceProvider);
        servicesByContract.computeIfAbsent(serviceType, it -> new TreeSet<>(ServiceProviderComparator.instance()))
                .add(serviceProvider);

        for (TypeName contract : serviceProvider.contracts()) {
            servicesByContract.computeIfAbsent(contract, it -> new TreeSet<>(ServiceProviderComparator.instance()))
                    .add(serviceProvider);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T> List lookup(Lookup criteria, int limit) {
        lookupCounter.increment();

        if (criteria.serviceType().isPresent()) {
            // when a specific service type is requested, we go for it
            ServiceProvider<?> exact = servicesByTypeName.get(criteria.serviceType().get());
            if (exact != null) {
                return explodeFilterAndSort(List.of(exact), criteria);
            }
        }

        if (1 == criteria.contracts().size()) {
            TypeName theOnlyContractRequested = criteria.contracts().iterator().next();
            Set<ServiceProvider<?>> subsetOfMatches = servicesByContract.get(theOnlyContractRequested);
            if (subsetOfMatches != null) {
                List<ServiceProvider<?>> result = subsetOfMatches.stream()
                        .parallel()
                        .filter(criteria::matches)
                        .limit(limit)
                        .toList();
                if (!result.isEmpty()) {
                    return explodeFilterAndSort(result, criteria);
                }
            }
            if (criteria.serviceType().isEmpty()) {
                // we may have a request for service type and not a contract
                ServiceProvider<?> exact = servicesByTypeName.get(theOnlyContractRequested);
                if (exact != null) {
                    return explodeFilterAndSort(List.of(exact), criteria);
                }
            }
        }

        if (cfg.serviceLookupCaching()) {
            List result = cache.get(criteria);
            cacheLookupCounter.increment();
            if (result != null) {
                cacheHitCounter.increment();
                return result;
            }
        }

        // table scan :-(
        List result = servicesByTypeName.values()
                .stream()
                .parallel()
                .filter(criteria::matches)
                .limit(limit)
                .toList();

        if (!result.isEmpty()) {
            result = explodeFilterAndSort(result, criteria);
        }

        if (cfg.serviceLookupCaching()) {
            cache.put(criteria, result);
        }

        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private <T> List<Supplier<T>> explodeFilterAndSort(List<ServiceProvider<?>> coll, Lookup criteria) {
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

        if (exploded.size() > 1) {
            exploded.sort(ServiceProviderComparator.instance());
        }

        // the providers are sorted by weight and other properties
        // we need to have unnamed providers before named ones (if criteria does not contain a Named qualifier)
        // in similar fashion, if criteria does not contain any qualifier, put unqualified instances first
        if (criteria.qualifiers().isEmpty()) {
            // unqualified first, unnamed before named, but keep the existing order otherwise
            List unqualified = new ArrayList<>();
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
            List unnamed = new ArrayList<>();
            List<ServiceProvider<?>> named = new ArrayList<>();
            for (ServiceProvider serviceProvider : exploded) {
                if (hasNamed(serviceProvider.qualifiers())) {
                    named.add(serviceProvider);
                } else {
                    unnamed.add(serviceProvider);
                }
            }
            unnamed.addAll(named);
            return unnamed;
        }

        // need to coerce the compiler into the correct type here...
        return (List) exploded;
    }

    private class ServiceBinderImpl implements ServiceBinder {
        private final String moduleName;

        ServiceBinderImpl(String moduleName) {
            this.moduleName = moduleName;
        }

        @Override
        public void bind(ServiceDescriptor<?> serviceDescriptor) {
            ServicesImpl.this.bind(serviceDescriptor);
        }

        @Override
        public String toString() {
            return "Service binder for module: " + moduleName;
        }
    }

    private class AppBinderImpl implements ServiceInjectionPlanBinder {
        private final String appName;

        AppBinderImpl(String appName) {
            this.appName = appName;
        }

        @Override
        public Binder bindTo(ServiceInfo serviceInfo) {
            ServiceProvider<?> serviceProvider = ServicesImpl.this.serviceProvider(serviceInfo);

            Optional<Binder> binder = serviceProvider.serviceProviderBindable()
                    .flatMap(ServiceProviderBindable::injectionPlanBinder);

            if (binder.isEmpty()) {
                // basically this means this service will not support compile-time injection
                LOGGER.log(Level.WARNING,
                           "service provider is not capable of being bound to injection points: " + serviceProvider);
                return new NoOpBinder(serviceProvider);
            }

            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG, "binding injection plan to " + binder.get());
            }

            return binder.get();
        }

        @Override
        public void interceptors(ServiceInfo... serviceInfos) {
            ServicesImpl.this.interceptors(serviceInfos);
        }

        @Override
        public String toString() {
            return "Service binder for application: " + appName;
        }
    }

    private static class NoOpBinder implements ServiceInjectionPlanBinder.Binder {
        private final ServiceProvider<?> serviceProvider;

        NoOpBinder(ServiceProvider<?> serviceProvider) {
            this.serviceProvider = serviceProvider;
        }

        @Override
        public ServiceInjectionPlanBinder.Binder bind(Ip id, boolean useProvider, ServiceInfo serviceInfo) {
            return this;
        }

        @Override
        public ServiceInjectionPlanBinder.Binder bindOptional(Ip id, boolean useProvider, ServiceInfo... serviceInfos) {
            return this;
        }

        @Override
        public ServiceInjectionPlanBinder.Binder bindMany(Ip id, boolean useProvider, ServiceInfo... serviceInfos) {
            return this;
        }

        @Override
        public ServiceInjectionPlanBinder.Binder bindNull(Ip id) {
            return this;
        }

        @Override
        public ServiceInjectionPlanBinder.Binder runtimeBind(Ip id, boolean useProvider, Class<?> serviceType) {
            return this;
        }

        @Override
        public ServiceInjectionPlanBinder.Binder runtimeBindOptional(Ip id, boolean useProvider, Class<?> serviceType) {
            return this;
        }

        @Override
        public ServiceInjectionPlanBinder.Binder runtimeBindMany(Ip id, boolean useProvider, Class<?> serviceType) {
            return this;
        }

        @Override
        public ServiceInjectionPlanBinder.Binder runtimeBindNullable(Ip id, boolean useProvider, Class<?> serviceType) {
            return this;
        }

        @Override
        public void commit() {
        }

        @Override
        public String toString() {
            return "No-op binder for " + serviceProvider.description();
        }
    }
}
