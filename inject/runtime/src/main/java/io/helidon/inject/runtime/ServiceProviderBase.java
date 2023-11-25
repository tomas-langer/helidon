package io.helidon.inject.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.ActivationRequest;
import io.helidon.inject.api.ActivationResult;
import io.helidon.inject.api.ActivationStatus;
import io.helidon.inject.api.Activator;
import io.helidon.inject.api.ContextualServiceQuery;
import io.helidon.inject.api.DeActivationRequest;
import io.helidon.inject.api.InjectionContext;
import io.helidon.inject.api.InjectionPointProvider;
import io.helidon.inject.api.InjectionServiceProviderException;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.InterceptionMetadata;
import io.helidon.inject.api.IpId;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.ServiceDescriptor;
import io.helidon.inject.api.ServiceInfoCriteria;
import io.helidon.inject.api.ServiceInjectionPlanBinder;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceProviderBindable;
import io.helidon.inject.api.ServiceProviderInjectionException;
import io.helidon.inject.api.ServiceSource;
import io.helidon.inject.api.Services;
import io.helidon.inject.spi.InjectionResolver;

import jakarta.inject.Provider;

public abstract class ServiceProviderBase<T>
        extends DescribedServiceProvider<T>
        implements ServiceProviderBindable<T>, ServiceDescriptor<T>, Activator<T> {
    static final TypeName PROVIDER_TYPE = TypeName.create(Provider.class);
    private static final System.Logger LOGGER = System.getLogger(ServiceProviderBase.class.getName());
    private static final TypeName SERVICE_PROVIDER_TYPE = TypeName.create(ServiceProvider.class);
    private static final TypeName INJECTION_POINT_PROVIDER_TYPE = TypeName.create(InjectionPointProvider.class);
    private static final ContextualServiceQuery EMPTY_QUERY = ContextualServiceQuery.builder()
            .serviceInfoCriteria(InjectionServices.EMPTY_CRITERIA)
            .build();

    private final InjectionServices injectionServices;
    private final InterceptionMetadata interceptionMetadata;
    private final ServiceSource<T> source;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    private volatile ServiceInstance<T> serviceInstance;
    private volatile Phase currentPhase = Phase.CONSTRUCTED;
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public List<T> list(ContextualServiceQuery query) {
        T serviceOrProvider = get(query.expected());

        Object result;
        if (contracts().contains(PROVIDER_TYPE)) {
            if (contracts().contains(INJECTION_POINT_PROVIDER_TYPE)) {
                InjectionPointProvider<T> provider = (InjectionPointProvider<T>) serviceOrProvider;
                result = provider.list(query);
            } else if (contracts().contains(SERVICE_PROVIDER_TYPE)) {
                ServiceProvider<T> provider = (ServiceProvider<T>) serviceOrProvider;
                result = provider.list(query);
            } else {
                Provider<T> provider = (Provider<T>) serviceOrProvider;
                result = provider.get();
            }
        } else {
            result = serviceOrProvider;
        }

        if (result == null) {
            if (query.expected()) {
                throw new ServiceProviderInjectionException("This managed service instance expected to have been set",
                                                            this);
            }
            return List.of();
        }

        if (result instanceof List list) {
            return list;
        } else {
            return (List<T>) List.of(result);
        }
    }

    @Override
    public Optional<T> first(ContextualServiceQuery query) {
        T serviceOrProvider = get(query.expected());

        try {
            return first(query, serviceOrProvider);
        } catch (ServiceProviderInjectionException ie) {
            throw ie;
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.ERROR, "Unable to activate: " + descriptorType().fqName(), e);
            throw new InjectionServiceProviderException("Unable to activate: " + descriptorType().fqName(), e, this);
        }
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
    public Optional<ServiceInjectionPlanBinder.Binder> injectionPlanBinder() {
        if (injectionContext != null) {
            LOGGER.log(System.Logger.Level.WARNING,
                       "this service provider already has an injection plan (which is unusual here): " + this);
        }
        return Optional.of(new ServiceInjectBinderImpl(injectionServices, this));
    }

    @Override
    public String toString() {
        return description();
    }

    @Override
    public boolean isProvider() {
        return contracts().contains(PROVIDER_TYPE);
    }

    protected T get(boolean expected) {
        Lock lock = rwLock.readLock();
        try {
            lock.lock();
            if (currentPhase == Phase.ACTIVE) {
                return serviceInstance.get();
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
            return serviceInstance.get();
        } finally {
            lock.unlock();
        }
    }

    protected void state(Phase phase, T instance) {
        Lock lock = rwLock.writeLock();
        try {
            lock.lock();
            this.currentPhase = phase;
            if (this.serviceInstance == null) {
                this.serviceInstance = ServiceInstance.create(source, instance);
            }
        } finally {
            lock.unlock();
        }
    }

    protected ServiceSource<T> source() {
        return source;
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
        return descriptor().serviceType().classNameWithEnclosingNames();
    }

    protected InjectionServices injectionServices() {
        return injectionServices;
    }

    protected ActivationResult doActivate(ActivationRequest req) {
        Phase initialPhase = this.currentPhase;
        Phase startingPhase = req.startingPhase().orElse(initialPhase);
        Phase targetPhase = req.targetPhase();
        this.currentPhase = startingPhase;
        Phase finishingPhase = startingPhase;

        ActivationResult.Builder res = ActivationResult.builder()
                .serviceProvider(this)
                .startingActivationPhase(initialPhase)
                .finishingActivationPhase(startingPhase)
                .targetActivationPhase(targetPhase)
                .finishingStatus(ActivationStatus.SUCCESS);

        if (targetPhase.ordinal() >= Phase.INIT.ordinal() && initialPhase == Phase.CONSTRUCTED) {
            init(req, res);
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

        res.startingActivationPhase(this.currentPhase);
        stateTransitionStart(res, Phase.PRE_DESTROYING);
        preDestroy(req, res);
        stateTransitionStart(res, Phase.DESTROYED);

        return res.build();
    }

    protected void postConstruct(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.POST_CONSTRUCTING);

        if (serviceInstance != null) {
            serviceInstance.postConstruct();
        }
    }

    protected void inject(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.INJECTING);

        if (serviceInstance != null) {
            serviceInstance.inject();
        }
    }

    protected void construct(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.CONSTRUCTING);

        // descendant may set an explicit instance, in such a case, we will not re-create it
        if (serviceInstance == null) {
            serviceInstance = ServiceInstance.create(interceptionMetadata, injectionContext, source);
            serviceInstance.construct();
        }
    }

    protected void prepareDependency(Services services, Map<IpId, Supplier<?>> injectionPlan, IpId dependency) {
        ServiceInfoCriteria criteria = dependency.toCriteria();
        List<ServiceProvider<?>> discovered = services.lookupAll(criteria, false)
                .stream()
                .filter(it -> it != this)
                .toList();

        // todo: if empty, try to use injection point provider

        TypeName ipType = dependency.typeName();

        // now there are a few options - optional, list, and single instance
        if (discovered.isEmpty()) {
            if (ipType.isOptional()) {
                injectionPlan.put(dependency, Optional::empty);
                return;
            }
            if (ipType.isList()) {
                injectionPlan.put(dependency, List::of);
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
                injectionPlan.put(dependency, () -> discovered);
                return;
            }

            if (discovered.size() == 1) {
                injectionPlan.put(dependency, () -> {
                    Object resolved = discovered.getFirst().get();
                    if (resolved instanceof List<?>) {
                        return resolved;
                    }
                    return List.of(resolved);
                });
                return;
            }

            injectionPlan.put(dependency, () -> discovered.stream()
                    .map(ServiceProvider::get)
                    .toList());
            return;
        }
        if (ipType.isOptional()) {
            // is an Optional needed?
            TypeName typeOfElement = ipType.typeArguments().getFirst();
            if (typeOfElement.equals(PROVIDER_TYPE) || typeOfElement.equals(SERVICE_PROVIDER_TYPE)) {
                injectionPlan.put(dependency, () -> Optional.of(discovered.getFirst()));
                return;
            }

            injectionPlan.put(dependency, () -> {
                Optional<?> firstResult = discovered.getFirst().first(EMPTY_QUERY);
                if (firstResult.isEmpty()) {
                    return Optional.empty();
                }
                Object resolved = firstResult.get();
                if (resolved instanceof Optional<?>) {
                    return resolved;
                }
                return Optional.ofNullable(resolved);
            });
            return;
        }

        if (ipType.equals(PROVIDER_TYPE) || ipType.equals(SERVICE_PROVIDER_TYPE) || ipType.equals(INJECTION_POINT_PROVIDER_TYPE)) {
            // is a provider needed?
            injectionPlan.put(dependency, discovered::getFirst);
            return;
        }
        // and finally just get the value of the first service
        injectionPlan.put(dependency, discovered.getFirst()::get);
    }

    protected void stateTransitionStart(ActivationResult.Builder res, Phase phase) {
        res.finishingActivationPhase(phase);
        this.currentPhase = phase;
    }

    protected Optional<InjectionContext> injectionContext() {
        return Optional.ofNullable(injectionContext);
    }

    protected void injectionContext(InjectionContext injectionContext) {
        this.injectionContext = injectionContext;
    }

    protected void init(ActivationRequest req, ActivationResult.Builder res) {
        stateTransitionStart(res, Phase.INIT);
    }

    @SuppressWarnings("unchecked")
    private Optional<T> first(ContextualServiceQuery query, T serviceOrProvider) {
        T service;
        if (contracts().contains(PROVIDER_TYPE)) {
            if (contracts().contains(INJECTION_POINT_PROVIDER_TYPE)) {
                InjectionPointProvider<T> provider = (InjectionPointProvider<T>) serviceOrProvider;
                service = provider.first(query).orElse(null);
            } else if (contracts().contains(SERVICE_PROVIDER_TYPE)) {
                ServiceProvider<T> provider = (ServiceProvider<T>) serviceOrProvider;
                service = provider.first(query).orElse(null);
            } else {
                Provider<T> provider = (Provider<T>) serviceOrProvider;
                service = provider.get();
            }
        } else {
            service = serviceOrProvider;
        }

        if (service == null) {
            if (query.expected()) {
                throw new ServiceProviderInjectionException("This managed service instance expected to have been set",
                                                            this);
            }
            return Optional.empty();
        }
        return Optional.of(service);
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

        List<IpId> servicesDeps = dependencies();

        if (servicesDeps.isEmpty()) {
            return;
        }

        if (injectionContext != null) {
            // obtained from application
            return;
        }

        Map<IpId, Supplier<?>> injectionPlan = new HashMap<>();

        Services services = injectionServices.services();
        for (IpId ipInfo : servicesDeps) {
            prepareDependency(services, injectionPlan, ipInfo);
        }

        this.injectionContext = InjectionContext.create(injectionPlan);
    }

    protected void preDestroy(DeActivationRequest req, ActivationResult.Builder res) {
        if (serviceInstance != null) {
            serviceInstance.preDestroy();
            serviceInstance = null;
        }
    }

    protected static class ServiceInjectBinderImpl implements ServiceInjectionPlanBinder.Binder {
        private final InjectionServices injectionServices;
        private final ServiceProviderBase<?> self;
        private final Map<IpId, Supplier<?>> injectionPlan = new HashMap<>();
        private final Services services;

        protected ServiceInjectBinderImpl(InjectionServices services, ServiceProviderBase<?> self) {
            this.injectionServices = services;
            this.self = self;
            this.services = services.services();
        }

        @Override
        public void commit() {
            self.injectionContext(InjectionContext.create(injectionPlan));
        }

        @Override
        public ServiceInjectionPlanBinder.Binder bind(IpId id, boolean useProvider, ServiceDescriptor<?> descriptor) {
            ServiceProvider<?> serviceProvider = BoundServiceProvider.create(services.serviceProvider(descriptor), id);
            if (useProvider) {
                injectionPlan.put(id, () -> serviceProvider);
            } else {
                ContextualServiceQuery query = ContextualServiceQuery.builder()
                        .serviceInfoCriteria(id.toCriteria())
                        .expected(true)
                        .build();
                injectionPlan.put(id, () -> mapFromProvider(query, serviceProvider));
            }

            return this;
        }

        @Override
        public ServiceInjectionPlanBinder.Binder bindOptional(IpId id,
                                                              boolean useProvider,
                                                              ServiceDescriptor<?>... descriptor) {

            if (descriptor.length == 0) {
                injectionPlan.put(id, Optional::empty);
            } else {
                ServiceProvider<?> serviceProvider = BoundServiceProvider.create(services.serviceProvider(descriptor[0]), id);
                if (useProvider) {
                    injectionPlan.put(id, () -> Optional.of(serviceProvider));
                } else {
                    ContextualServiceQuery query = ContextualServiceQuery.builder()
                            .serviceInfoCriteria(id.toCriteria())
                            .build();
                    injectionPlan.put(id, () -> Optional.ofNullable(mapFromProvider(query, serviceProvider)));
                }
            }

            return this;
        }

        @Override
        public ServiceInjectionPlanBinder.Binder bindMany(IpId id,
                                                          boolean useProvider,
                                                          ServiceDescriptor<?>... descriptors) {

            List<? extends ServiceProvider<?>> providers = Stream.of(descriptors)
                    .map(services::serviceProvider)
                    .map(it -> BoundServiceProvider.create(it, id))
                    .toList();

            if (useProvider) {
                injectionPlan.put(id, () -> providers);
            } else {
                ContextualServiceQuery query = ContextualServiceQuery.builder()
                        .serviceInfoCriteria(id.toCriteria())
                        .expected(true)
                        .build();
                injectionPlan.put(id, () -> providers.stream()
                        .flatMap(it -> mapStreamFromProvider(query, it))
                        .toList());
            }

            return this;
        }

        @Override
        public ServiceInjectionPlanBinder.Binder bindNull(IpId id) {
            injectionPlan.put(id, () -> null);
            return this;
        }

        @Override
        public ServiceInjectionPlanBinder.Binder runtimeBind(IpId id, boolean useProvider, Class<?> serviceType) {
            if (self instanceof InjectionResolver ir) {
                Optional<IpId> foundIp = self.dependencies()
                        .stream()
                        .filter(it -> it == id)
                        .findFirst();

                if (foundIp.isPresent()) {
                    injectionPlan.put(id, () -> ir.resolve(foundIp.get(), injectionServices, self, true).get());
                    return this;
                }
            }
            ServiceProvider<?> serviceProvider = services.lookup(serviceType);
            injectionPlan.put(id, () -> useProvider ? serviceProvider : serviceProvider.get());
            return this;
        }

        @Override
        public ServiceInjectionPlanBinder.Binder runtimeBindOptional(IpId id, boolean useProvider, Class<?> serviceType) {
            if (self instanceof InjectionResolver ir) {
                Optional<IpId> foundIp = self.dependencies()
                        .stream()
                        .filter(it -> it == id)
                        .findFirst();

                if (foundIp.isPresent()) {
                    injectionPlan.put(id, () -> ir.resolve(foundIp.get(), injectionServices, self, true));
                    return this;
                }
            }
            Optional<? extends ServiceProvider<?>> serviceProvider = services.lookupFirst(serviceType, false);
            if (serviceProvider.isEmpty()) {
                injectionPlan.put(id, Optional::empty);
            } else {
                injectionPlan.put(id, () -> useProvider ? Optional.of(serviceProvider) : Optional.of(serviceProvider.get()));
            }

            return this;
        }

        @Override
        public ServiceInjectionPlanBinder.Binder runtimeBindMany(IpId id, boolean useProvider, Class<?> serviceType) {
            List<? extends ServiceProvider<?>> providers = services.lookupAll(serviceType);

            if (useProvider) {
                injectionPlan.put(id, () -> providers);
            } else {
                injectionPlan.put(id, () -> providers.stream()
                        .map(ServiceProvider::get)
                        .toList());
            }

            return this;
        }

        @Override
        public ServiceInjectionPlanBinder.Binder runtimeBindNullable(IpId id, boolean useProvider, Class<?> serviceType) {
            Optional<? extends ServiceProvider<?>> serviceProvider = services.lookupFirst(serviceType, false);

            if (serviceProvider.isEmpty()) {
                injectionPlan.put(id, () -> null);
            } else {
                injectionPlan.put(id, () -> useProvider ? serviceProvider : serviceProvider.get());
            }

            return this;
        }

        protected Map<IpId, Supplier<?>> injectionPlan() {
            return injectionPlan;
        }

        private Object mapFromProvider(ContextualServiceQuery query, ServiceProvider<?> provider) {
            return provider.first(query).orElse(null);
        }

        private Stream<?> mapStreamFromProvider(ContextualServiceQuery query, ServiceProvider<?> provider) {
            return provider.list(query).stream();
        }
    }
}
