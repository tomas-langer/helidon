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
import io.helidon.inject.api.ContextualServiceQuery;
import io.helidon.inject.api.DeActivationRequest;
import io.helidon.inject.api.InjectionContext;
import io.helidon.inject.api.InjectionPointProvider;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.InterceptionMetadata;
import io.helidon.inject.api.IpId;
import io.helidon.inject.api.IpInfo;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.ServiceDependencies;
import io.helidon.inject.api.ServiceDescriptor;
import io.helidon.inject.api.ServiceInfoCriteria;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceProviderBindable;
import io.helidon.inject.api.ServiceProviderInjectionException;
import io.helidon.inject.api.ServiceSource;
import io.helidon.inject.api.Services;

import jakarta.inject.Provider;

public abstract class ServiceProviderBase<T>
        extends DescribedServiceProvider<T>
        implements ServiceProviderBindable<T>, ServiceDescriptor<T>, Activator<T> {
    private static final TypeName PROVIDER_TYPE = TypeName.create(Provider.class);
    private static final TypeName SERVICE_PROVIDER_TYPE = TypeName.create(ServiceProvider.class);

    private final InjectionServices injectionServices;
    private final InterceptionMetadata interceptionMetadata;
    private final ServiceSource<T> source;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    private volatile T serviceInstance;
    private volatile Phase currentPhase = Phase.INIT;
    private volatile ServiceProvider<?> interceptor;
    private volatile InjectionContext injectionContext;

    protected ServiceProviderBase(InjectionServices injectionServices,
                                  ServiceSource<T> serviceSource) {
        super(serviceSource);

        this.injectionServices = injectionServices;
        this.interceptionMetadata = new InterceptionMetadataImpl(injectionServices);
        this.source = serviceSource;
    }

    @Override
    public ActivationResult activate(ActivationRequest activationRequest) {
        // acquire write lock, as this is expected to activate
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            if (currentPhase == activationRequest.targetPhase()) {
                return ActivationResult.builder()
                        .serviceProvider(this)
                        .startingActivationPhase(currentPhase)
                        .finishingActivationPhase(currentPhase)
                        .targetActivationPhase(currentPhase)
                        .finishingStatus(ActivationStatus.SUCCESS)
                        .build();
            }
            if (currentPhase.ordinal() > activationRequest.targetPhase().ordinal()) {
                return ActivationResult.builder()
                        .serviceProvider(this)
                        .startingActivationPhase(currentPhase)
                        .finishingActivationPhase(currentPhase)
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
    public ServiceProvider<T> serviceProvider() {
        return this;
    }

    @Override
    public Optional<T> first(ContextualServiceQuery ctx) {
        T serviceOrProvider = get(ctx.expected());

        T service;
        if (serviceOrProvider == null) {
            service = null;
        } else {
            if (isProvider()) {
                service = fromProvider(ctx, serviceOrProvider);
            } else {
                service = serviceOrProvider;
            }
        }

        if (service == null) {
            if (ctx.expected()) {
                throw new ServiceProviderInjectionException("This managed service instance expected to have been set",
                                                            this);
            }
            return Optional.empty();
        }

        return Optional.of(service);
    }

    @Override
    public String id() {
        return id(true);
    }

    @Override
    public String description() {
        return id(false) + ":" + currentActivationPhase();
    }

    @Override
    public Phase currentActivationPhase() {
        return currentPhase;
    }

    @Override
    public Optional<ServiceProviderBindable<T>> serviceProviderBindable() {
        return Optional.of(this);
    }

    @Override
    public void interceptor(ServiceProvider<?> interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public boolean isIntercepted() {
        return interceptor != null;
    }

    @Override
    public Optional<ServiceProvider<?>> interceptor() {
        return Optional.ofNullable(interceptor);
    }

    @Override
    public String toString() {
        return description();
    }

    protected T get(boolean expected) {
        Lock lock = rwLock.readLock();
        try {
            lock.lock();
            if (currentPhase == Phase.ACTIVE) {
                return serviceInstance;
            }
        } finally {
            lock.unlock();
        }
        ActivationResult res = activate(InjectionServices.createActivationRequestDefault());
        if (res.failure() && expected) {
            throw new ServiceProviderInjectionException("Activation failed: " + res, this);
        }
        try {
            lock.lock();
            return get();
        } finally {
            lock.unlock();
        }
    }

    protected void state(Phase phase, T instance) {
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            this.currentPhase = phase;
            this.serviceInstance = instance;
        } finally {
            lock.unlock();
        }
    }
    protected void instance(T instance) {
        Lock lock = rwLock.writeLock();
        try {
            this.serviceInstance = instance;
        } finally {
            lock.unlock();
        }
    }

    protected void phase(Phase phase) {
        Lock lock = rwLock.writeLock();
        try {
            this.currentPhase = phase;
        } finally {
            lock.unlock();
        }
    }

    protected String id(boolean fq) {
        if (fq) {
            return descriptor().serviceType().fqName();
        }
        return descriptor().serviceType().className();
    }

    protected InjectionServices injectionServices() {
        return injectionServices;
    }

    protected ActivationResult doActivate(ActivationRequest req) {
        Phase initialPhase = this.currentPhase;
        Phase startingPhase = req.startingPhase().orElse(Phase.PENDING);
        Phase targetPhase = req.targetPhase();
        this.currentPhase = startingPhase;
        Phase finishingPhase = startingPhase;

        ActivationResult.Builder res = ActivationResult.builder()
                .serviceProvider(this)
                .startingActivationPhase(initialPhase)
                .finishingActivationPhase(startingPhase)
                .targetActivationPhase(targetPhase)
                .finishingStatus(ActivationStatus.SUCCESS);

        if (targetPhase.ordinal() >= Phase.PENDING.ordinal() && initialPhase == Phase.INIT) {
            pending(req, res);
        }
        finishingPhase = res.finishingActivationPhase().orElse(finishingPhase);

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

    protected ActivationResult doDeactivate(DeActivationRequest req) {
        ActivationResult.Builder res = ActivationResult.builder()
                .serviceProvider(this)
                .finishingStatus(ActivationStatus.SUCCESS);

        if (!currentPhase.eligibleForDeactivation()) {
            stateTransitionStart(res, Phase.DESTROYED);
            return ActivationResult.builder()
                    .serviceProvider(this)
                    .targetActivationPhase(Phase.DESTROYED)
                    .finishingStatus(ActivationStatus.SUCCESS)
                    .build();
        }

        stateTransitionStart(res, Phase.PRE_DESTROYING);
        preDestroy(req, res);
        stateTransitionStart(res, Phase.DESTROYED);

        return res.build();
    }

    protected void postConstruct(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.POST_CONSTRUCTING);

        if (serviceInstance != null) {
            source.postConstruct(serviceInstance);
        }
    }

    protected void inject(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.INJECTING);

        if (serviceInstance != null) {
            source.injectFields(injectionContext, interceptionMetadata, serviceInstance);
            source.injectMethods(injectionContext, serviceInstance);
        }
    }

    @SuppressWarnings("unchecked") // we have unchecked here, so generated code can be checked
    protected void construct(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.CONSTRUCTING);

        // descendant may set an explicit instance, in such a case, we will not re-create it
        if (serviceInstance == null) {
            serviceInstance = (T) source.instantiate(injectionContext, interceptionMetadata);
        }
    }

    protected void prepareDependency(Services services, Map<IpId<?>, Supplier<?>> injectionPlan, IpInfo dependency) {
        IpId<?> id = dependency.id();

        ServiceInfoCriteria criteria = toCriteria(dependency);
        List<ServiceProvider<?>> discovered = services.lookupAll(criteria, false)
                .stream()
                .filter(it -> it != this)
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
                                                                + ", for service: " + serviceType().fqName(),
                                                        this);
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

    protected void stateTransitionStart(ActivationResult.Builder res, Phase phase) {
        res.startingActivationPhase(currentPhase)
                .finishingActivationPhase(phase);
        this.currentPhase = phase;
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

    protected void pending(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.PENDING);
    }

    private void gatherDependencies(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.GATHERING_DEPENDENCIES);

        List<ServiceDependencies> servicesDeps = dependencies();

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

    private void preDestroy(DeActivationRequest req, ActivationResult.Builder res) {
        if (serviceInstance != null) {
            source.preDestroy(serviceInstance);
            serviceInstance = null;
        }
    }

    @SuppressWarnings("unchecked")
    private T fromProvider(ContextualServiceQuery ctx, T serviceOrProvider) {
        if (serviceOrProvider instanceof InjectionPointProvider<?> ipp) {
            return (T) ipp.first(ctx).orElse(null);
        }
        if (serviceOrProvider instanceof Provider<?> provider) {
            return (T) provider.get();
        }
        throw new UnsupportedOperationException("Not yet implemented in new world");
        // return NonSingletonServiceProvider.createAndActivate(this);
    }

    private ServiceInfoCriteria toCriteria(IpInfo dependency) {
        return ServiceInfoCriteria.builder()
                .addContract(dependency.contract())
                .qualifiers(dependency.qualifiers())
                .build();
    }
}
