package io.helidon.inject.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import io.helidon.inject.api.ActivationRequest;
import io.helidon.inject.api.ActivationResult;
import io.helidon.inject.api.ActivationStatus;
import io.helidon.inject.api.Activator;
import io.helidon.inject.api.DeActivationRequest;
import io.helidon.inject.api.DeActivator;
import io.helidon.inject.api.InjectionContext;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.IpId;
import io.helidon.inject.api.IpInfo;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.ServiceDescriptor;
import io.helidon.inject.api.ServiceProvider;

class InjectServiceActivator<T> implements Activator, DeActivator {
    private final ServiceDescriptor<T> descriptor;
    private final ServiceProvider<T> provider;
    private final InjectionServices injectionServices;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    private volatile Phase activationPhase = Phase.INIT;
    private volatile T serviceInstance;
    private volatile InjectionContext injectionContext;

    InjectServiceActivator(InjectionServices injectionServices,
                           ServiceProvider<T> provider,
                           ServiceDescriptor<T> descriptor) {
        this.injectionServices = injectionServices;
        this.provider = provider;
        this.descriptor = descriptor;
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

    private ActivationResult doDeactivate(DeActivationRequest req) {
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

    private ActivationResult doActivate(ActivationRequest req) {
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

    private void setActive(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.ACTIVE);
    }

    private void finishActivation(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.ACTIVATION_FINISHING);
    }

    private void postConstruct(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.POST_CONSTRUCTING);

        descriptor.postConstruct(serviceInstance);
    }

    private void inject(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.INJECTING);

        descriptor.injectFields(injectionContext, serviceInstance);
        descriptor.injectMethods(injectionContext, serviceInstance);
    }

    private void construct(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.CONSTRUCTING);

        serviceInstance = descriptor.instantiate(injectionContext);
    }

    private void startLifecycle(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.ACTIVATION_STARTING);
    }

    private void gatherDependencies(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.GATHERING_DEPENDENCIES);

        List<IpInfo<?>> dependencies = descriptor.dependencies();

        if (dependencies.isEmpty()) {
            this.injectionContext = InjectionContext.empty();
            return;
        }
        Map<IpId<?>, Supplier<?>> injectionPlan = new HashMap<>();
        for (IpInfo<?> dependency : dependencies) {
            prepareDependency(injectionPlan, dependency);
        }

        this.injectionContext = new InjectionContextImpl(injectionPlan);
    }

    private void prepareDependency(Map<IpId<?>, Supplier<?>> injectionPlan, IpInfo<?> dependency) {

    }

    private void preDestroy(DeActivationRequest req, ActivationResult.Builder res) {
        descriptor.preDestroy(serviceInstance);
        serviceInstance = null;
    }


    void stateTransitionStart(ActivationResult.Builder res, Phase phase) {
        res.finishingActivationPhase(phase);
        this.activationPhase = phase;
    }
}
