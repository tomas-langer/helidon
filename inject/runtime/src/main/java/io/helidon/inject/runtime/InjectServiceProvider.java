package io.helidon.inject.runtime;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.inject.api.Activator;
import io.helidon.inject.api.ContextualServiceQuery;
import io.helidon.inject.api.DeActivator;
import io.helidon.inject.api.DependenciesInfo;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.PostConstructMethod;
import io.helidon.inject.api.PreDestroyMethod;
import io.helidon.inject.api.ServiceDescriptor;
import io.helidon.inject.api.ServiceInfo;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceProviderBindable;

class InjectServiceProvider<T> implements ServiceProviderBindable<T> {
    private final AtomicReference<T> serviceRef = new AtomicReference<>();

    public InjectServiceProvider(InjectionServices injectionServices, ServiceDescriptor<T> descriptor) {

    }

    static <T> InjectServiceProvider<T> create(InjectionServices injectionServices, ServiceDescriptor<T> descriptor) {
        return new InjectServiceProvider<>(injectionServices, descriptor);
    }

    @Override
    public Optional<T> first(ContextualServiceQuery ctx) {
        return Optional.empty();
        /*
        ActivationResult res = ensureActivated();

        if (res.failure() && ctx.expected()) {
            throw new ServiceProviderInjectionException("Activation failed: " + res, this);
        }

        T instance = serviceRef.get();
        if (instance == null && ctx.expected()) {
            throw new ServiceProviderInjectionException("This managed service instance expected to have been set", e);
        }

        return Optional.ofNullable(instance);

         */
    }


    @Override
    public String id() {
        return null;
    }

    @Override
    public String description() {
        return null;
    }

    @Override
    public boolean isProvider() {
        return false;
    }

    @Override
    public ServiceInfo serviceInfo() {
        return null;
    }

    @Override
    public DependenciesInfo dependencies() {
        return null;
    }

    @Override
    public Phase currentActivationPhase() {
        return null;
    }

    @Override
    public Optional<Activator> activator() {
        return Optional.empty();
    }

    @Override
    public Optional<DeActivator> deActivator() {
        return Optional.empty();
    }

    @Override
    public Optional<PostConstructMethod> postConstructMethod() {
        return Optional.empty();
    }

    @Override
    public Optional<PreDestroyMethod> preDestroyMethod() {
        return Optional.empty();
    }

    @Override
    public Optional<ServiceProviderBindable<T>> serviceProviderBindable() {
        return Optional.empty();
    }

    @Override
    public Class<?> serviceType() {
        return null;
    }

    @Override
    public void moduleName(String moduleName) {

    }

    @Override
    public Optional<ServiceProvider<?>> interceptor() {
        return Optional.empty();
    }

    @Override
    public Optional<InjectionServices> injectionServices() {
        return Optional.empty();
    }

    @Override
    public void injectionServices(Optional<InjectionServices> injectionServices) {

    }
}
