package io.helidon.inject;

import java.lang.System.Logger.Level;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import io.helidon.common.HelidonServiceLoader;
import io.helidon.common.types.TypeName;
import io.helidon.inject.service.ModuleComponent;
import io.helidon.inject.service.ServiceInfo;

class InjectionServicesImpl extends ResettableHandler implements InjectionServices, Resettable {
    private static final System.Logger LOGGER = System.getLogger(InjectionServices.class.getName());
    private static final ReadWriteLock INSTANCE_LOCK = new ReentrantReadWriteLock();
    private static final AtomicReference<InjectionConfig> CONFIG = new AtomicReference<>();

    private static volatile InjectionServicesImpl instance;

    private final State state = State.create(Phase.INIT);
    private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock();
    private final InjectionConfig config;

    private volatile ServicesImpl services;

    private InjectionServicesImpl(InjectionConfig config) {
        this.config = config;
    }

    static InjectionServices instance() {
        Lock lock = INSTANCE_LOCK.readLock();
        try {
            lock.lock();
            if (instance != null) {
                return instance;
            }
        } finally {
            lock.unlock();
        }
        lock = INSTANCE_LOCK.writeLock();
        try {
            lock.lock();
            InjectionConfig config = CONFIG.get();
            if (config == null) {
                config = InjectionConfig.create();
            }
            instance = new InjectionServicesImpl(config);
            ResettableHandler.addResettable(instance);

            return instance;
        } finally {
            lock.unlock();
        }
    }

    static void configure(InjectionConfig config) {
        CONFIG.set(config);
    }

    @Override
    public Services services() {
        Lock readLock = lifecycleLock.readLock();
        try {
            readLock.lock();
            if (services != null) {
                return services;
            }
        } finally {
            readLock.unlock();
        }

        Lock writeLock = lifecycleLock.writeLock();
        try {
            writeLock.lock();
            if (services != null) {
                return services;
            }
            state.currentPhase(Phase.ACTIVATION_STARTING);
            services = new ServicesImpl(this, state);
            services.bindSelf();

            if (config.useModules()) {
                List<ModuleComponent> modules = findModules();
                modules.forEach(services::bind);
            }

            state.currentPhase(Phase.GATHERING_DEPENDENCIES);
            if (config.useApplication()) {
                List<Application> apps = findApplications();
                apps.forEach(services::bind);
            }

            List<ActivationPhaseReceiver> phaseReceivers = services.allProviders()
                    .stream()
                    .filter(sp -> sp instanceof ActivationPhaseReceiver)
                    .map(ActivationPhaseReceiver.class::cast)
                    .toList();

            state.currentPhase(Phase.POST_BIND_ALL_MODULES);
            phaseReceivers.forEach(sp -> sp.onPhaseEvent(Phase.POST_BIND_ALL_MODULES));

            state.currentPhase(Phase.FINAL_RESOLVE);
            phaseReceivers.forEach(sp -> sp.onPhaseEvent(Phase.FINAL_RESOLVE));

            state.currentPhase(Phase.SERVICES_READY);
            phaseReceivers.forEach(sp -> sp.onPhaseEvent(Phase.SERVICES_READY));

            state.finished(true);

            return services;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public InjectionConfig config() {
        return config;
    }

    @Override
    public Map<TypeName, ActivationResult> shutdown() {
        Lock lock = lifecycleLock.writeLock();
        try {
            lock.lock();
            if (services == null) {
                return Map.of();
            }
            State currentState = state.clone().currentPhase(Phase.PRE_DESTROYING);
            return doShutdown(services, currentState);
        } finally {
            services = null;
            lock.unlock();
        }
    }

    @Override
    public void reset(boolean deep) {
        INSTANCE_LOCK.writeLock().lock();
        try {
            CONFIG.set(null);
            instance = null;
        } finally {
            INSTANCE_LOCK.writeLock().unlock();
        }

        // now reset this instance
        Lock lock = lifecycleLock.writeLock();
        try {
            lock.lock();

            if (!config.permitsDynamic()) {
                throw new IllegalStateException(
                        "Attempting to rest InjectionServices that do not support dynamic updates. Set option permitsDynamic, "
                                + "or configuration option 'inject.permits-dynamic=true' to enable");
            }

            if (services != null) {
                services = null;
            }
        } finally {
            lock.unlock();
        }
    }

    private static Map<TypeName, ActivationResult> doShutdown(ServicesImpl services, State state) {
        Map<TypeName, ActivationResult> result = new LinkedHashMap<>();

        state.currentPhase(Phase.DESTROYED);

        // next get all services that are beyond INIT state, and sort by runlevel order, and shut those down also
        List<ServiceProvider<?>> serviceProviders = services.allProviders();
        serviceProviders = serviceProviders.stream()
                .filter(sp -> sp.currentActivationPhase().eligibleForDeactivation())
                .collect(Collectors.toList()); // must be a mutable list, as we sort it in next step
        serviceProviders.sort(shutdownComparator());
        doFinalShutdown(services, serviceProviders, result);

        return result;
    }

    private static Comparator<? super ServiceProvider<?>> shutdownComparator() {
        return Comparator.comparingInt(ServiceInfo::runLevel)
                .thenComparing(ServiceInfo::weight);
    }

    private static void doFinalShutdown(ServicesImpl services,
                                        Collection<ServiceProvider<?>> serviceProviders,
                                        Map<TypeName, ActivationResult> map) {
        for (ServiceProvider<?> csp : serviceProviders) {
            Phase startingActivationPhase = csp.currentActivationPhase();
            try {
                Activator<?> activator;
                Optional<Activator<?>> activatorOptional = services.activator(csp);
                if (activatorOptional.isPresent()) {
                    activator = activatorOptional.get();
                } else {
                    if (csp instanceof Activator<?> cspa) {
                        activator = cspa;
                    } else {
                        ActivationResult result = ActivationResult.builder()
                                .serviceProvider(csp)
                                .startingActivationPhase(startingActivationPhase)
                                .targetActivationPhase(Phase.DESTROYED)
                                .finishingActivationPhase(csp.currentActivationPhase())
                                .finishingStatus(ActivationStatus.FAILURE)
                                .error(new InjectionException("Failed to discover activator for the service provider,"
                                                                      + " cannot shut down"))
                                .build();
                        map.put(csp.serviceType(), result);
                        continue;
                    }
                }
                ActivationResult result = activator.deactivate(DeActivationRequest.builder()
                                                                       .throwIfError(false)
                                                                       .build());
                map.put(csp.serviceType(), result);
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Failed to deactivate service provider: " + csp, t);
                ActivationResult result = ActivationResult.builder()
                        .serviceProvider(csp)
                        .startingActivationPhase(startingActivationPhase)
                        .targetActivationPhase(Phase.DESTROYED)
                        .finishingActivationPhase(csp.currentActivationPhase())
                        .finishingStatus(ActivationStatus.FAILURE)
                        .error(t)
                        .build();
                map.put(csp.serviceType(), result);
            }
        }
    }

    private List<Application> findApplications() {
        return HelidonServiceLoader.create(ServiceLoader.load(Application.class))
                .asList();
    }

    private List<ModuleComponent> findModules() {
        return HelidonServiceLoader.create(ServiceLoader.load(ModuleComponent.class))
                .asList();
    }

}
