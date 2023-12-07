package io.helidon.inject.runtime;

import io.helidon.inject.api.Activator;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.service.Descriptor;

class InjectServiceProvider<T> extends ServiceProviderBase<T> {
    protected InjectServiceProvider(InjectionServices injectionServices, Descriptor<T> serviceSource) {
        super(injectionServices, serviceSource);
    }

    static <T> Activator<T> create(InjectionServices injectionServices, Descriptor<T> descriptor) {
        return new InjectServiceProvider<>(injectionServices, descriptor);
    }
}
