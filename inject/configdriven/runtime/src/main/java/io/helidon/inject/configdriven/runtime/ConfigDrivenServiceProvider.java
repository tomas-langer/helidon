package io.helidon.inject.configdriven.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import io.helidon.common.config.Config;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.TypeName;
import io.helidon.inject.api.ActivationPhaseReceiver;
import io.helidon.inject.api.ActivationRequest;
import io.helidon.inject.api.ActivationResult;
import io.helidon.inject.api.CommonQualifiers;
import io.helidon.inject.api.ContextualServiceQuery;
import io.helidon.inject.api.Event;
import io.helidon.inject.api.InjectionPointInfo;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.Qualifier;
import io.helidon.inject.api.ServiceDescriptor;
import io.helidon.inject.api.ServiceInfo;
import io.helidon.inject.api.ServiceInfoCriteria;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceProviderInjectionException;
import io.helidon.inject.api.ServiceProviderProvider;
import io.helidon.inject.configdriven.api.ConfigBeanFactory;
import io.helidon.inject.configdriven.api.ConfigDriven;
import io.helidon.inject.configdriven.api.NamedInstance;
import io.helidon.inject.runtime.ServiceProviderBase;
import io.helidon.inject.spi.InjectionResolver;

import static io.helidon.inject.api.CommonQualifiers.WILDCARD_NAMED;
import static io.helidon.inject.configdriven.runtime.ConfigDrivenUtils.hasValue;
import static io.helidon.inject.configdriven.runtime.ConfigDrivenUtils.isBlank;
import static java.lang.System.Logger.Level.DEBUG;

class ConfigDrivenServiceProvider<T, CB> extends ServiceProviderBase<T,
        ConfigDrivenServiceProvider<T, CB>,
        ConfigDrivenServiceActivator<T, CB>>
        implements ConfiguredServiceProvider<T, CB>,
                   InjectionResolver,
                   ServiceProviderProvider,
                   ActivationPhaseReceiver {
    private static final System.Logger LOGGER = System.getLogger(ConfigDrivenServiceProvider.class.getName());
    private static final Qualifier EMPTY_CONFIGURED_BY = Qualifier.create(ConfigDriven.class);

    private final AtomicBoolean registeredWithCbr = new AtomicBoolean();
    // map of name to config driven service provider
    private final Map<String, ConfigDrivenInstanceProvider<T, CB>> managedConfiguredServicesMap
            = new ConcurrentHashMap<>();
    private final List<ConfigBeanServiceProvider<CB>> managedConfigBeans = new ArrayList<>();

    ConfigDrivenServiceProvider(InjectionServices injectionServices, ServiceDescriptor<T> descriptor) {
        super(injectionServices,
              descriptor,
              new ConfigDrivenServiceActivator<>(injectionServices, descriptor),
              ServiceInfo.builder()
                      .update(it -> descriptor.contracts().forEach(it::addContractImplemented))
                      .addContractImplemented(descriptor.serviceType())
                      .scopeTypeNames(descriptor.scopes())
                      .qualifiers(descriptor.qualifiers())
                      .declaredRunLevel(descriptor.runLevel())
                      .declaredWeight(descriptor.weight())
                      .serviceTypeName(descriptor.serviceType())
                      .addQualifier(CommonQualifiers.WILDCARD_NAMED)
                      .build());
    }

    public static <T> ServiceProvider<T> create(InjectionServices injectionServices, ServiceDescriptor<T> descriptor) {
        return new ConfigDrivenServiceProvider<T, Object>(injectionServices, descriptor);
    }

    @Override
    public boolean isProvider() {
        // this is a provider of config beans, not the target instance
        return true;
    }

    // note that all responsibilities to resolve is delegated to the root provider
    @Override
    public Optional<Object> resolve(InjectionPointInfo ipInfo,
                                    InjectionServices injectionServices,
                                    ServiceProvider<?> serviceProvider,
                                    boolean resolveIps) {
        if (resolveIps) {
            // too early to resolve...
            return Optional.empty();
        }

        ServiceInfoCriteria dep = ipInfo.dependencyToServiceInfo();
        ServiceInfoCriteria criteria = ServiceInfoCriteria.builder()
                .addContractImplemented(configBeanType())
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
        Object prev = managedConfiguredServicesMap.put(configBean.name(),
                                                       new ConfigDrivenInstanceProvider<>(getInjectionServices(),
                                                                                          descriptor(),
                                                                                          this,
                                                                                          configBean.name(),
                                                                                          configBean.instance()));
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
                && hasValue(configuredByQualifier.get().value().orElse(null));
        boolean blankCriteria = qualifiers.isEmpty() && isBlank(criteria);
        boolean managedQualify = !managedConfiguredServicesMap.isEmpty()
                && (blankCriteria || hasValue || configuredByQualifier.isEmpty());
        boolean rootQualifies = wantThis
                && (
                blankCriteria
                        || (
                        managedConfiguredServicesMap.isEmpty()
                                && (
                                qualifiers.isEmpty()
                                        || qualifiers.contains(WILDCARD_NAMED))
                                || (!hasValue && configuredByQualifier.isPresent())));

        boolean serviceTypeMatch = serviceInfo().matches(criteria);
        if (managedQualify) {
            List<ServiceProvider<?>> result = new ArrayList<>();

            if (criteria.contractsImplemented().contains(TypeName.create(configBeanType()))) {
                for (ConfigBeanServiceProvider<CB> managedConfigBean : managedConfigBeans) {
                    if (managedConfigBean.serviceInfo().matches(criteria)) {
                        result.add(managedConfigBean);
                    }
                }
            }
            if (serviceTypeMatch) {
                result.addAll(new ArrayList<>(managedServiceProviders(criteria)
                                                      .values()));
            }

            if (rootQualifies && serviceTypeMatch) {
                if (thisAlreadyMatches || serviceInfo().matches(criteria)) {
                    result.add(this);
                }
                // no need to sort using the comparator here since we should already be in the proper order...
                return result;
            } else {
                return result;
            }
        } else if (rootQualifies
                && (thisAlreadyMatches || serviceInfo().matches(criteria))) {
            if (!hasValue && managedConfiguredServicesMap.isEmpty()) {
                // TODO: this used UnconfiguredServiceProvider - is it needed? First should still return nothing if this has no
                //  beans
                return List.of(this);
            }
            return List.of(this);
        }

        return List.of();
    }

    @Override
    public Map<String, ConfigDrivenInstanceProvider<?, CB>> managedServiceProviders(ServiceInfoCriteria criteria) {
        Map<String, ConfigDrivenInstanceProvider<?, CB>> map = managedConfiguredServicesMap.entrySet()
                .stream()
                .filter(e -> e.getValue().serviceInfo().matches(criteria))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        if (map.size() <= 1) {
            return map;
        }

        Map<String, ConfigDrivenInstanceProvider<?, CB>> result = new TreeMap<>(NameComparator.instance());
        result.putAll(map);
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<T> first(ContextualServiceQuery ctx) {
        // we are root provider
        if (currentActivationPhase() != Phase.ACTIVE) {
            // we know the activator is present, as we send it through constructor...
            ActivationResult res = super.activator()
                    .get()
                    .activate(ActivationRequest.builder().targetPhase(InjectionServices.terminalActivationPhase()).build());
            if (res.failure()) {
                if (ctx.expected()) {
                    throw new ServiceProviderInjectionException("Activation failed: " + res, this);
                }
                return Optional.empty();
            }
        }

        ServiceInfoCriteria criteria = ctx.serviceInfoCriteria();
        List<ServiceProvider<?>> qualifiedProviders = serviceProviders(criteria, false, true);
        for (ServiceProvider<?> qualifiedProvider : qualifiedProviders) {
            assert (this != qualifiedProvider);
            Optional<?> serviceOrProvider = qualifiedProvider.first(ctx);
            if (serviceOrProvider.isPresent()) {
                return (Optional<T>) serviceOrProvider;
            }
        }

        if (ctx.expected()) {
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
        getActivator().onPhaseEvent(event, phase);
    }

    @Override
    public void injectionServices(Optional<InjectionServices> injectionServices) {
        if (registeredWithCbr.compareAndSet(false, true)) {
            ConfigBeanRegistryImpl cbr = ConfigBeanRegistryImpl.CONFIG_BEAN_REGISTRY.get();
            if (cbr != null) {
                Optional<Qualifier> configuredByQualifier = descriptor().qualifiers().stream()
                        .filter(q -> q.typeName().name().equals(ConfigDriven.class.getName()))
                        .findFirst();
                assert (configuredByQualifier.isPresent());
                cbr.bind(this, configuredByQualifier.get());
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<NamedInstance<CB>> createConfigBeans(Config config) {
        // we know this is the case, as otherwise the ID would be wrong
        ConfigBeanFactory<CB> factory = (ConfigBeanFactory<CB>) descriptor();
        return factory.createConfigBeans(config);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean drivesActivation() {
        return ((ConfigBeanFactory<CB>) descriptor()).drivesActivation();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<CB> configBeanType() {
        // we know this is the case, as otherwise the ID would be wrong
        ConfigBeanFactory<CB> factory = (ConfigBeanFactory<CB>) descriptor();
        return factory.configBeanType();
    }

    boolean hasManagedServices() {
        return !managedConfigBeans.isEmpty();
    }

    @Override
    protected String id(boolean fq) {
        return super.id(fq) + "{root}";
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

            csp.activator()
                    .ifPresent(it -> it.activate(ActivationRequest.builder()
                                                         .targetPhase(Phase.PENDING)
                                                         .build()));
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
}
