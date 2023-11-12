package io.helidon.inject.runtime;

import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.ServiceDescriptor;

class InjectServiceActivator<T> extends ServiceActivatorBase<T, InjectServiceProvider<T>> {
    InjectServiceActivator(InjectionServices injectionServices,
                           ServiceDescriptor<T> descriptor) {
        super(injectionServices, descriptor);
    }
}
