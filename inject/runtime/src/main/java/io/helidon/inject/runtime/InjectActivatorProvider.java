package io.helidon.inject.runtime;

import io.helidon.common.Weighted;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.ServiceDescriptor;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceSource;
import io.helidon.inject.spi.ActivatorProvider;

class InjectActivatorProvider implements ActivatorProvider, Weighted {
    InjectActivatorProvider() {
    }

    @Override
    public String id() {
        return ServiceDescriptor.INJECTION_RUNTIME_ID;
    }

    @Override
    public <T> ServiceProvider<T> activator(InjectionServices injectionServices, ServiceSource<T> descriptor) {
        return InjectServiceProvider.create(injectionServices, descriptor);
    }

    @Override
    public double weight() {
        // less than default, so others can override it
        return Weighted.DEFAULT_WEIGHT - 10;
    }
}
