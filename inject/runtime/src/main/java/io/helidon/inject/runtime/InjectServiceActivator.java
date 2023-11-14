package io.helidon.inject.runtime;

import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.ServiceSource;

class InjectServiceActivator<T> extends ServiceActivatorBase<T, InjectServiceProvider<T>> {
    InjectServiceActivator(InjectionServices injectionServices,
                           ServiceSource<T> descriptor) {
        super(injectionServices, descriptor);
    }
}
