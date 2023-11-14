package io.helidon.inject.runtime;

import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.ServiceSource;

class InjectServiceProvider<T> extends ServiceProviderBase<T, InjectServiceProvider<T>, InjectServiceActivator<T>> {
    InjectServiceProvider(InjectionServices injectionServices, ServiceSource<T> descriptor) {
        super(injectionServices,
              descriptor,
              new InjectServiceActivator<>(injectionServices, descriptor));
    }

    static <T> InjectServiceProvider<T> create(InjectionServices injectionServices, ServiceSource<T> descriptor) {
        return new InjectServiceProvider<>(injectionServices, descriptor);
    }
}
