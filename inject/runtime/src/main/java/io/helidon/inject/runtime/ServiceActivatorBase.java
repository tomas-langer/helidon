package io.helidon.inject.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.ActivationRequest;
import io.helidon.inject.api.ActivationResult;
import io.helidon.inject.api.ActivationStatus;
import io.helidon.inject.api.Activator;
import io.helidon.inject.api.DeActivationRequest;
import io.helidon.inject.api.DeActivator;
import io.helidon.inject.api.InjectionContext;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.InterceptionMetadata;
import io.helidon.inject.api.IpId;
import io.helidon.inject.api.IpInfo;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.ServiceDependencies;
import io.helidon.inject.api.ServiceDescriptor;
import io.helidon.inject.api.ServiceInfoCriteria;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceProviderInjectionException;
import io.helidon.inject.api.ServiceSource;
import io.helidon.inject.api.Services;

import jakarta.inject.Provider;

public abstract class ServiceActivatorBase<T, S extends ServiceProviderBase<T, S, ?>> implements Activator, DeActivator {
    private static final TypeName PROVIDER_TYPE = TypeName.create(Provider.class);
    private static final TypeName SERVICE_PROVIDER_TYPE = TypeName.create(ServiceProvider.class);

    private final ServiceSource<T> descriptor;
    private final InjectionServices injectionServices;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final InterceptionMetadata interceptionMetadata;

    private volatile S provider;
    private volatile Phase activationPhase = Phase.INIT;
    private volatile T serviceInstance;
    private volatile InjectionContext injectionContext;

    protected ServiceActivatorBase(InjectionServices injectionServices,
                                   ServiceSource<T> descriptor) {

        this.injectionServices = injectionServices;
        this.descriptor = descriptor;
        this.interceptionMetadata = new InterceptionMetadataImpl(injectionServices);
    }

    @Override
    public ActivationResult activate(ActivationRequest activationRequest) {
        // acquire write lock, as this is expected to activate
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            if (activationPhase == activationRequest.targetPhase()) {
                return ActivationResult.builder()
                        .serviceProvider(provider)
                        .startingActivationPhase(activationPhase)
                        .finishingActivationPhase(activationPhase)
                        .targetActivationPhase(activationPhase)
                        .finishingStatus(ActivationStatus.SUCCESS)
                        .build();
            }
            if (activationPhase.ordinal() > activationRequest.targetPhase().ordinal()) {
                return ActivationResult.builder()
                        .serviceProvider(provider)
                        .startingActivationPhase(activationPhase)
                        .finishingActivationPhase(activationPhase)
                        .targetActivationPhase(activationRequest.targetPhase())
                        .finishingStatus(ActivationStatus.FAILURE)
                        .build();
            }
            return doActivate(activationRequest);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ActivationResult deactivate(DeActivationRequest req) {
        // acquire write lock, as this is expected to de-activate
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            return doDeactivate(req);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String toString() {
        return "Service activator for: " + descriptor().serviceType().fqName() + "[" + phase() + "]";
    }

    protected ServiceDescriptor<T> descriptor() {
        return descriptor;
    }

    protected Phase phase() {
        return activationPhase;
    }

    protected T get(boolean expected) {
        Lock lock = rwLock.readLock();
        try {
            lock.lock();
            if (activationPhase == Phase.ACTIVE) {
                return serviceInstance;
            }
        } finally {
            lock.unlock();
        }
        ActivationResult res = activate(InjectionServices.createActivationRequestDefault());
        if (res.failure() && expected) {
            throw new ServiceProviderInjectionException("Activation failed: " + res, provider);
        }
        try {
            lock.lock();
            return serviceInstance;
        } finally {
            lock.unlock();
        }
    }

    protected void stateTransitionStart(ActivationResult.Builder res, Phase phase) {
        res.finishingActivationPhase(phase);
        this.activationPhase = phase;
    }

    protected ActivationResult doDeactivate(DeActivationRequest req) {
        if (!activationPhase.eligibleForDeactivation()) {
            return ActivationResult.builder()
                    .serviceProvider(provider)
                    .startingActivationPhase(activationPhase)
                    .finishingActivationPhase(activationPhase)
                    .targetActivationPhase(activationPhase)
                    .finishingStatus(ActivationStatus.SUCCESS)
                    .build();
        }

        ActivationResult.Builder res = ActivationResult.builder();
        stateTransitionStart(res, Phase.PRE_DESTROYING);
        preDestroy(req, res);
        this.activationPhase = Phase.DESTROYED;

        return res.build();
    }

    protected ActivationResult doActivate(ActivationRequest req) {
        Phase initialPhase = this.activationPhase;
        Phase startingPhase = req.startingPhase().orElse(Phase.PENDING);
        Phase targetPhase = req.targetPhase();
        this.activationPhase = startingPhase;
        Phase finishingPhase = startingPhase;

        ActivationResult.Builder res = ActivationResult.builder()
                .serviceProvider(provider)
                .startingActivationPhase(initialPhase)
                .finishingActivationPhase(startingPhase)
                .targetActivationPhase(targetPhase);

        if (targetPhase.ordinal() > Phase.ACTIVATION_STARTING.ordinal()) {
            if (Phase.INIT == startingPhase
                    || Phase.PENDING == startingPhase
                    || Phase.ACTIVATION_STARTING == startingPhase
                    || Phase.DESTROYED == startingPhase) {
                startLifecycle(req, res);
            }
        }
        finishingPhase = res.finishingActivationPhase().orElse(finishingPhase);
        if (targetPhase.ordinal() > Phase.GATHERING_DEPENDENCIES.ordinal()
                && Phase.ACTIVATION_STARTING == finishingPhase) {
            gatherDependencies(req, res);
        }
        finishingPhase = res.finishingActivationPhase().orElse(finishingPhase);
        if (res.targetActivationPhase().ordinal() >= Phase.CONSTRUCTING.ordinal()
                && (Phase.GATHERING_DEPENDENCIES == finishingPhase)) {
            construct(req, res);
        }
        finishingPhase = res.finishingActivationPhase().orElse(finishingPhase);
        if (res.targetActivationPhase().ordinal() >= Phase.INJECTING.ordinal()
                && (Phase.CONSTRUCTING == finishingPhase)) {
            inject(req, res);
        }
        finishingPhase = res.finishingActivationPhase().orElse(finishingPhase);
        if (res.targetActivationPhase().ordinal() >= Phase.POST_CONSTRUCTING.ordinal()
                && (Phase.INJECTING == finishingPhase)) {
            postConstruct(req, res);
        }
        finishingPhase = res.finishingActivationPhase().orElse(finishingPhase);
        if (res.targetActivationPhase().ordinal() >= Phase.ACTIVATION_FINISHING.ordinal()
                && (Phase.POST_CONSTRUCTING == finishingPhase)) {
            finishActivation(req, res);
        }
        finishingPhase = res.finishingActivationPhase().orElse(finishingPhase);
        if (res.targetActivationPhase().ordinal() >= Phase.ACTIVE.ordinal()
                && (Phase.ACTIVATION_FINISHING == finishingPhase)) {
            setActive(req, res);
        }

        return res.build();
    }

    void serviceProvider(S provider) {
        this.provider = provider;
    }

    protected S provider() {
        return provider;
    }

    protected void prepareDependency(Services services, Map<IpId<?>, Supplier<?>> injectionPlan, IpInfo dependency) {
        IpId<?> id = dependency.id();

        ServiceInfoCriteria criteria = toCriteria(dependency);
        List<ServiceProvider<?>> discovered = services.lookupAll(criteria, false)
                .stream()
                .filter(it -> it != provider)
                .toList();

        // todo: if empty, try to use injection point provider

        TypeName ipType = id.typeName();

        // now there are a few options - optional, list, and single instance
        if (discovered.isEmpty()) {
            if (ipType.isOptional()) {
                injectionPlan.put(id, Optional::empty);
                return;
            }
            if (ipType.isList()) {
                injectionPlan.put(id, List::of);
                return;
            }
            throw new ServiceProviderInjectionException("Expected to resolve a service matching "
                                                                + criteria
                                                                + " for dependency: " + dependency
                                                                + ", for service: " + descriptor.serviceType().fqName(),
                                                        descriptor);
        }

        // we have a response
        if (ipType.isList()) {
            // is a list needed?
            TypeName typeOfElements = ipType.typeArguments().getFirst();
            if (typeOfElements.equals(PROVIDER_TYPE) || typeOfElements.equals(SERVICE_PROVIDER_TYPE)) {
                injectionPlan.put(id, () -> discovered);
                return;
            }

            if (discovered.size() == 1) {
                injectionPlan.put(id, () -> {
                    Object resolved = discovered.getFirst().get();
                    if (resolved instanceof List<?>) {
                        return resolved;
                    }
                    return List.of(resolved);
                });
                return;
            }

            injectionPlan.put(id, () -> discovered.stream()
                    .map(ServiceProvider::get)
                    .toList());
            return;
        }
        if (ipType.isOptional()) {
            // is an Optional needed?
            TypeName typeOfElement = ipType.typeArguments().getFirst();
            if (typeOfElement.equals(PROVIDER_TYPE) || typeOfElement.equals(SERVICE_PROVIDER_TYPE)) {
                injectionPlan.put(id, () -> Optional.of(discovered.getFirst()));
                return;
            }

            injectionPlan.put(id, () -> {
                Object resolved = discovered.getFirst().get();
                if (resolved instanceof Optional<?>) {
                    return resolved;
                }
                return Optional.ofNullable(resolved);
            });
            return;
        }

        if (ipType.equals(PROVIDER_TYPE) || ipType.equals(SERVICE_PROVIDER_TYPE)) {
            // is a provider needed?
            injectionPlan.put(id, discovered::getFirst);
            return;
        }
        // and finally just get the value of the first service
        injectionPlan.put(id, discovered.getFirst()::get);
    }

    protected void postConstruct(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.POST_CONSTRUCTING);

        if (serviceInstance != null) {
            descriptor.postConstruct(serviceInstance);
        }
    }

    protected void inject(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.INJECTING);

        if (serviceInstance != null) {
            descriptor.injectFields(injectionContext, interceptionMetadata, serviceInstance);
            descriptor.injectMethods(injectionContext, serviceInstance);
        }
    }

    @SuppressWarnings("unchecked") // we have unchecked here, so generated code can be checked
    protected void construct(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.CONSTRUCTING);

        // descendant may set an explicit instance, in such a case, we will not re-create it
        if (serviceInstance == null) {
            serviceInstance = (T) descriptor.instantiate(injectionContext, interceptionMetadata);
        }
    }

    protected void phase(Phase phase) {
        this.activationPhase = phase;
    }

    protected void instance(T instance) {
        this.serviceInstance = instance;
    }

    private void setActive(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.ACTIVE);
    }

    private void finishActivation(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.ACTIVATION_FINISHING);
    }

    private void startLifecycle(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.ACTIVATION_STARTING);
    }

    private void gatherDependencies(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.GATHERING_DEPENDENCIES);

        List<ServiceDependencies> servicesDeps = descriptor.dependencies();

        Map<TypeName, Map<IpId<?>, Supplier<?>>> injectionPlans = new HashMap<>();

        for (ServiceDependencies service : servicesDeps) {
            List<IpInfo> dependencies = service.dependencies();

            if (dependencies.isEmpty()) {
                continue;
            }
            Map<IpId<?>, Supplier<?>> injectionPlan = new HashMap<>();
            Services services = injectionServices.services();
            for (IpInfo dependency : dependencies) {
                prepareDependency(services, injectionPlan, dependency);
            }
            injectionPlans.put(service.serviceType(), injectionPlan);
        }

        this.injectionContext = InjectionContext.create(injectionPlans);
    }

    private ServiceInfoCriteria toCriteria(IpInfo dependency) {
        return ServiceInfoCriteria.builder()
                .addContractImplemented(dependency.contract())
                .qualifiers(dependency.qualifiers())
                .build();
    }

    private void preDestroy(DeActivationRequest req, ActivationResult.Builder res) {
        if (serviceInstance != null) {
            descriptor.preDestroy(serviceInstance);
            serviceInstance = null;
        }
    }
}
