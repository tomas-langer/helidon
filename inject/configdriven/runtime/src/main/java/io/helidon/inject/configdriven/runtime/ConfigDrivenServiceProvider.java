/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.inject.configdriven.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.helidon.common.config.Config;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.TypeName;
import io.helidon.inject.api.ActivationPhaseReceiver;
import io.helidon.inject.api.ActivationRequest;
import io.helidon.inject.api.ActivationResult;
import io.helidon.inject.api.ActivationStatus;
import io.helidon.inject.api.Activator;
import io.helidon.inject.api.ContextualServiceQuery;
import io.helidon.inject.api.DeActivationRequest;
import io.helidon.inject.api.Event;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.ServiceInfoCriteria;
import io.helidon.inject.api.ServiceInjectionPlanBinder;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceProviderInjectionException;
import io.helidon.inject.api.ServiceProviderProvider;
import io.helidon.inject.api.Services;
import io.helidon.inject.configdriven.api.ConfigBeanFactory;
import io.helidon.inject.configdriven.api.ConfigDriven;
import io.helidon.inject.configdriven.api.NamedInstance;
import io.helidon.inject.runtime.HelidonInjectionContext;
import io.helidon.inject.runtime.ServiceProviderBase;
import io.helidon.inject.service.Descriptor;
import io.helidon.inject.service.InjectionContext;
import io.helidon.inject.service.IpId;
import io.helidon.inject.service.Qualifier;
import io.helidon.inject.spi.InjectionResolver;

import static java.lang.System.Logger.Level.DEBUG;

class ConfigDrivenServiceProvider<T, CB> extends ServiceProviderBase<T>
        implements ConfiguredServiceProvider<T, CB>,
                   InjectionResolver,
                   ServiceProviderProvider,
                   ActivationPhaseReceiver,
                   Activator<T> {
    private static final System.Logger LOGGER = System.getLogger(ConfigDrivenServiceProvider.class.getName());
    private static final Qualifier EMPTY_CONFIGURED_BY = Qualifier.create(ConfigDriven.class);

    private final AtomicBoolean registeredWithCbr = new AtomicBoolean();
    // map of name to config driven service provider
    private final Map<String, ConfigDrivenInstanceProvider<T, CB>> managedConfiguredServicesMap
            = new ConcurrentHashMap<>();
    private final List<ConfigBeanServiceProvider<CB>> managedConfigBeans = new ArrayList<>();
    private final Set<Qualifier> qualifiers;
    private final Descriptor<T> descriptor;

    private volatile ConfigDrivenBinderImpl cdInjectionContext;

    ConfigDrivenServiceProvider(InjectionServices injectionServices, Descriptor<T> descriptor) {
        super(injectionServices, descriptor);

        this.descriptor = descriptor;
        Set<Qualifier> qualifiers = new LinkedHashSet<>(descriptor.qualifiers());
        qualifiers.add(Qualifier.WILDCARD_NAMED);
        this.qualifiers = Set.copyOf(qualifiers);
    }

    static <T> Activator<T> create(InjectionServices injectionServices, Descriptor<T> descriptor) {
        return new ConfigDrivenServiceProvider<>(injectionServices, descriptor);
    }

    @Override
    public boolean isProvider() {
        // this is a provider of config beans, not the target instance
        return true;
    }

    // note that all responsibilities to resolve is delegated to the root provider
    @Override
    public Optional<Object> resolve(IpId ipInfo,
                                    InjectionServices injectionServices,
                                    ServiceProvider<?> serviceProvider,
                                    boolean resolveIps) {
        if (resolveIps) {
            // too early to resolve...
            return Optional.empty();
        }

        ServiceInfoCriteria dep = ServiceInfoCriteria.create(ipInfo);
        ServiceInfoCriteria criteria = ServiceInfoCriteria.builder()
                .addContract(configBeanType())
                .build();
        if (!dep.matchesContracts(criteria)) {
            return Optional.empty();    // we are being injected with neither a config bean nor a service that matches ourselves
        }

        // if we are here then we are asking for a config bean for ourselves, or a managed instance
        if (!dep.qualifiers().isEmpty()) {
            throw new ServiceProviderInjectionException("Cannot use qualifiers while injecting config beans for self", this);
        }

        return Optional.of(configBeanType());
    }

    /**
     * Called during initialization to register a loaded config bean.
     *
     * @param configBean the config bean
     */
    @Override
    public void registerConfigBean(NamedInstance<CB> configBean) {
        Objects.requireNonNull(configBean);

        ConfigBeanServiceProvider<CB> configBeanProvider = new ConfigBeanServiceProvider<>(configBeanType(),
                                                                                           configBean.instance(),
                                                                                           configBean.name());
        managedConfigBeans.add(configBeanProvider);

        ConfigDrivenInstanceProvider<T, CB> cdInstanceProvider
                = new ConfigDrivenInstanceProvider<>(injectionServices(),
                                                     descriptor,
                                                     this,
                                                     configBean.name(),
                                                     configBean.instance());
        Object prev = managedConfiguredServicesMap.put(configBean.name(),
                                                       cdInstanceProvider);

        if (cdInjectionContext != null) {
            cdInstanceProvider.injectionContext(cdInjectionContext.forInstance(cdInstanceProvider));
        }

        assert (prev == null);
    }

    @Override
    public List<ServiceProvider<?>> serviceProviders(ServiceInfoCriteria criteria,
                                                     boolean wantThis,
                                                     boolean thisAlreadyMatches) {
        /*
        the request may be for either:
        - Root service provider (the config driven type)
        - Managed service provider (driven by config beans)
        - Config bean itself
        */

        Set<Qualifier> qualifiers = criteria.qualifiers();
        Optional<? extends Annotation> configuredByQualifier = Annotations
                .findFirst(EMPTY_CONFIGURED_BY.typeName(), qualifiers);
        boolean hasValue = configuredByQualifier.isPresent()
                && !(configuredByQualifier.get().value().orElse("").isBlank());
        boolean blankCriteria = qualifiers.isEmpty()
                && criteria.qualifiers().isEmpty()
                && criteria.serviceTypeName().isEmpty()
                && criteria.contracts().isEmpty();
        boolean managedQualify = !managedConfiguredServicesMap.isEmpty()
                && (blankCriteria || hasValue || configuredByQualifier.isEmpty());
        boolean rootQualifies = wantThis
                && (
                blankCriteria
                        || (
                        managedConfiguredServicesMap.isEmpty()
                                && (
                                qualifiers.isEmpty()
                                        || qualifiers.contains(Qualifier.WILDCARD_NAMED))
                                || (!hasValue && configuredByQualifier.isPresent())));

        boolean serviceTypeMatch = criteria.matches(this);
        if (managedQualify) {
            List<ServiceProvider<?>> result = new ArrayList<>();

            if (criteria.contracts().contains(configBeanType())) {
                for (ConfigBeanServiceProvider<CB> managedConfigBean : managedConfigBeans) {
                    if (criteria.matches(managedConfigBean)) {
                        result.add(managedConfigBean);
                    }
                }
            }
            if (serviceTypeMatch) {
                result.addAll(new ArrayList<>(managedServiceProviders(criteria)
                                                      .values()));
            }

            if (rootQualifies && serviceTypeMatch) {
                if (thisAlreadyMatches || criteria.matches(this)) {
                    result.add(this);
                }
                // no need to sort using the comparator here since we should already be in the proper order...
                return result;
            } else {
                return result;
            }
        } else if (rootQualifies && (thisAlreadyMatches || criteria.matches(this))) {
            if (!hasValue && managedConfiguredServicesMap.isEmpty()) {
                return List.of(this);
            }
            return List.of(this);
        }

        return List.of();
    }

    @Override
    public Map<String, ConfigDrivenInstanceProvider<?, CB>> managedServiceProviders(ServiceInfoCriteria criteria) {
        // managed instances are always named, so the criteria must provide either wildcard named qualifier
        // or we add @default name (if none)

        Map<String, ConfigDrivenInstanceProvider<?, CB>> map = managedConfiguredServicesMap.entrySet()
                .stream()
                .filter(e -> criteria.matches(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (map.size() <= 1) {
            return map;
        }

        Map<String, ConfigDrivenInstanceProvider<?, CB>> result = new TreeMap<>(NamedInstance.nameComparator());
        result.putAll(map);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<T> first(ContextualServiceQuery query) {
        // we are root provider
        if (currentActivationPhase() != Phase.ACTIVE) {
            // we know the activator is present, as we send it through constructor...
            ActivationResult res = super.activate(ActivationRequest.builder()
                                                          .targetPhase(InjectionServices.terminalActivationPhase()).build());
            if (res.failure()) {
                if (query.expected()) {
                    throw new ServiceProviderInjectionException("Activation failed: " + res, this);
                }
                return Optional.empty();
            }
        }

        ServiceInfoCriteria criteria = query.serviceInfoCriteria();
        List<ServiceProvider<?>> qualifiedProviders = serviceProviders(criteria, false, true);
        for (ServiceProvider<?> qualifiedProvider : qualifiedProviders) {
            assert (this != qualifiedProvider);
            Optional<?> serviceOrProvider = qualifiedProvider.first(query);
            if (serviceOrProvider.isPresent()) {
                return (Optional<T>) serviceOrProvider;
            }
        }

        if (query.expected()) {
            throw new ServiceProviderInjectionException("Expected to find a match", this);
        }

        return Optional.empty();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<T> list(ContextualServiceQuery query) {
        // we are root
        Map<String, ConfigDrivenInstanceProvider<?, CB>> matching = managedServiceProviders(query.serviceInfoCriteria());
        if (!matching.isEmpty()) {
            List<?> result = matching.values().stream()
                    .map(it -> it.first(query))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!result.isEmpty()) {
                return (List<T>) result;
            }
        }

        if (!query.expected()) {
            return List.of();
        }

        throw new ServiceProviderInjectionException("Expected to return a non-null instance for: "
                                                            + query.injectionPointInfo()
                                                            + "; with criteria matching: " + query.serviceInfoCriteria(), this);
    }

    @Override
    public void onPhaseEvent(Event event,
                             Phase phase) {

        if (phase == Phase.POST_BIND_ALL_MODULES) {
            ActivationResult.Builder res = ActivationResult.builder();

            if (Phase.INIT == currentActivationPhase()) {
                stateTransitionStart(res, Phase.PENDING);
            }
        } else if (phase == Phase.FINAL_RESOLVE) {
            // post-initialize ourselves
            if (drivesActivation()) {
                activate(InjectionServices.createActivationRequestDefault());
            }

            resolveConfigDrivenServices();
        } else if (phase == Phase.SERVICES_READY) {
            activateConfigDrivenServices();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<NamedInstance<CB>> createConfigBeans(Config config) {
        // we know this is the case, as otherwise the ID would be wrong
        ConfigBeanFactory<CB> factory = (ConfigBeanFactory<CB>) serviceInfo();
        return factory.createConfigBeans(config);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean drivesActivation() {
        return ((ConfigBeanFactory<CB>) serviceInfo()).drivesActivation();
    }

    @SuppressWarnings("unchecked")
    @Override
    public TypeName configBeanType() {
        // we know this is the case, as otherwise the ID would be wrong
        ConfigBeanFactory<CB> factory = (ConfigBeanFactory<CB>) serviceInfo();
        return factory.configBeanType();
    }

    @Override
    public Set<Qualifier> qualifiers() {
        return qualifiers;
    }

    @Override
    public Optional<ServiceInjectionPlanBinder.Binder> injectionPlanBinder() {
        if (injectionContext().isPresent()) {
            LOGGER.log(System.Logger.Level.WARNING,
                       "this service provider already has an injection plan (which is unusual here): " + this);
        }
        return Optional.of(new ConfigDrivenBinderImpl(injectionServices(), this));
    }

    boolean hasManagedServices() {
        return !managedConfigBeans.isEmpty();
    }

    @Override
    protected String id(boolean fq) {
        return super.id(fq) + "{root}";
    }

    @Override
    protected void prepareDependency(Services services, Map<IpId, Supplier<?>> injectionPlan, IpId dependency) {
        // do nothing, config driven root service CANNOT be instantiated, as it does not have
        // a config bean to inject
    }

    @Override
    protected void construct(ActivationRequest req, ActivationResult.Builder res) {
        // do nothing, config driven root service CANNOT be instantiated, as it does not have a config bean

        if (!(hasManagedServices() && drivesActivation())) {
            stateTransitionStart(res, Phase.PENDING);
        } else {
            stateTransitionStart(res, Phase.CONSTRUCTING);
        }
    }

    @Override
    protected void init(ActivationRequest req, ActivationResult.Builder res) {
        super.init(req, res);
        if (registeredWithCbr.compareAndSet(false, true)) {
            ConfigBeanRegistryImpl cbr = ConfigBeanRegistryImpl.CONFIG_BEAN_REGISTRY.get();
            if (cbr != null) {
                Optional<Qualifier> configuredByQualifier = serviceInfo().qualifiers().stream()
                        .filter(q -> q.typeName().name().equals(ConfigDriven.class.getName()))
                        .findFirst();
                assert (configuredByQualifier.isPresent());
                cbr.bind(this, configuredByQualifier.get());
            }
        }
    }

    @Override
    protected void preDestroy(DeActivationRequest req, ActivationResult.Builder res) {
        // we never have an instance (this is a config driven root)
        // but we do have child instances
        for (ConfigDrivenInstanceProvider<T, CB> value : managedConfiguredServicesMap.values()) {
            ActivationResult result = value.deactivate(req);
            if (result.failure() && !(res.finishingStatus().map(it -> it != ActivationStatus.FAILURE).orElse(false))) {
                // record first failure
                res.serviceProvider(value);
                res.finishingStatus(result.finishingStatus());
            }
        }
    }

    void resolveConfigDrivenServices() {
        if (managedConfiguredServicesMap.isEmpty()) {
            if (LOGGER.isLoggable(DEBUG)) {
                LOGGER.log(DEBUG, "No configured services for: " + description());
            }
            return;
        }

        // accept and resolve config
        managedConfiguredServicesMap.values().forEach(csp -> {
            if (LOGGER.isLoggable(DEBUG)) {
                LOGGER.log(DEBUG, "Resolving config for " + csp);
            }

            csp.activate(ActivationRequest.builder()
                                 .targetPhase(Phase.INIT)
                                 .build());
        });

    }

    void activateConfigDrivenServices() {
        if (managedConfiguredServicesMap.isEmpty()) {
            return;
        }

        if (!drivesActivation()) {
            if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
                LOGGER.log(System.Logger.Level.DEBUG, "drivesActivation disabled for: " + description());
            }
            return;
        }

        managedConfiguredServicesMap.values().forEach(ConfigDrivenInstanceProvider::activate);
    }

    protected static class ConfigDrivenBinderImpl extends ServiceProviderBase.ServiceInjectBinderImpl {
        private final ConfigDrivenServiceProvider<?, ?> self;
        private final List<RuntimeBind> beanInstanceBindings = new ArrayList<>();

        protected ConfigDrivenBinderImpl(InjectionServices services, ConfigDrivenServiceProvider<?, ?> self) {
            super(services, self);
            this.self = self;
        }

        @Override
        public ServiceInjectionPlanBinder.Binder runtimeBind(IpId id, boolean useProvider, Class<?> serviceType) {

            if (id.contract().equals(self.configBeanType()) && id.qualifiers().isEmpty()) {
                beanInstanceBindings.add(new RuntimeBind(id, useProvider, false));
                return this;
            }

            return super.runtimeBind(id, useProvider, serviceType);
        }

        @Override
        public ServiceInjectionPlanBinder.Binder runtimeBindOptional(IpId id, boolean useProvider, Class<?> serviceType) {

            if (id.contract().equals(self.configBeanType()) && id.qualifiers().isEmpty()) {
                beanInstanceBindings.add(new RuntimeBind(id, useProvider, false));
                return this;
            }

            return super.runtimeBindOptional(id, useProvider, serviceType);
        }

        @Override
        public void commit() {
            super.commit();

            self.cdInjectionContext = this;
        }

        InjectionContext forInstance(ConfigDrivenInstanceProvider<?, ?> provider) {
            Map<IpId, Supplier<?>> copy = new HashMap<>(injectionPlan());
            for (RuntimeBind beanInstanceBinding : beanInstanceBindings) {
                Supplier<?> supplier;
                if (beanInstanceBinding.optional()) {
                    if (beanInstanceBinding.provider()) {
                        supplier = () -> Optional.of(provider);
                    } else {
                        supplier = () -> Optional.of(provider.beanInstance());
                    }
                } else {
                    if (beanInstanceBinding.provider()) {
                        supplier = () -> provider;
                    } else {
                        supplier = provider::beanInstance;
                    }
                }
                copy.put(beanInstanceBinding.id(), supplier);
            }

            return HelidonInjectionContext.create(copy);
        }

        private record RuntimeBind(IpId id, boolean provider, boolean optional) {
        }
    }
}
