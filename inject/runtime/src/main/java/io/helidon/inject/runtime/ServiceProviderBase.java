package io.helidon.inject.runtime;

import java.util.Optional;

import io.helidon.inject.api.Activator;
import io.helidon.inject.api.ContextualServiceQuery;
import io.helidon.inject.api.DeActivator;
import io.helidon.inject.api.DependenciesInfo;
import io.helidon.inject.api.InjectionPointProvider;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.PostConstructMethod;
import io.helidon.inject.api.PreDestroyMethod;
import io.helidon.inject.api.ServiceDescriptor;
import io.helidon.inject.api.ServiceInfo;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceProviderBindable;
import io.helidon.inject.api.ServiceProviderInjectionException;

import jakarta.inject.Provider;

public abstract class ServiceProviderBase<T, S extends ServiceProviderBase<T, S, ?>, A extends ServiceActivatorBase<T, S>>
        implements ServiceProviderBindable<T> {
    private final A activator;
    private final ServiceInfo serviceInfo;
    private final ServiceDescriptor<T> descriptor;
    private final InjectionServices injectionServices;

    private volatile ServiceProvider<?> interceptor;

    @SuppressWarnings("unchecked")
    protected ServiceProviderBase(InjectionServices injectionServices,
                                  ServiceDescriptor<T> descriptor,
                                  A activator,
                                  ServiceInfo serviceInfo) {
        this.injectionServices = injectionServices;
        this.descriptor = descriptor;
        this.activator = activator;
        this.serviceInfo = serviceInfo;
        // must be done post construction of this instance
        this.activator.serviceProvider((S) this);
    }

    @Override
    public Optional<T> first(ContextualServiceQuery ctx) {
        T serviceOrProvider = activator.get(ctx.expected());

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
    public boolean isProvider() {
        return false;
    }

    @Override
    public ServiceInfo serviceInfo() {
        return serviceInfo;
    }

    @Override
    public void moduleName(String moduleName) {
    }

    @Override
    public Class<?> serviceType() {
        return descriptor.serviceType();
    }

    @Override
    public Phase currentActivationPhase() {
        return activator.phase();
    }

    @Override
    public Optional<Activator> activator() {
        return Optional.of(activator);
    }

    @Override
    public Optional<DeActivator> deActivator() {
        return Optional.of(activator);
    }

    @Override
    public Optional<InjectionServices> injectionServices() {
        return Optional.of(injectionServices);
    }

    @Override
    public void injectionServices(Optional<InjectionServices> injectionServices) {
    }

    @Override
    public Optional<PostConstructMethod> postConstructMethod() {
        return Optional.of(() -> descriptor.postConstruct(activator.get(true)));
    }

    @Override
    public Optional<PreDestroyMethod> preDestroyMethod() {
        return Optional.of(() -> descriptor.preDestroy(activator.get(true)));
    }

    @Override
    public DependenciesInfo dependencies() {
        return DependenciesInfo.create();
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

    protected String id(boolean fq) {
        if (fq) {
            return descriptor.serviceType().getName();
        }
        return descriptor.serviceType().getSimpleName();
    }

    protected ServiceDescriptor<T> descriptor() {
        return descriptor;
    }

    protected InjectionServices getInjectionServices() {
        return injectionServices;
    }

    protected A getActivator() {
        return activator;
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
}
