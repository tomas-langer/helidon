package io.helidon.inject.runtime;

import io.helidon.common.Weighted;
import io.helidon.inject.api.Activator;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.service.Descriptor;
import io.helidon.inject.service.ServiceInfo;
import io.helidon.inject.spi.ActivatorProvider;

class InjectActivatorProvider implements ActivatorProvider, Weighted {
    InjectActivatorProvider() {
    }

    @Override
    public String id() {
        return ServiceInfo.INJECTION_RUNTIME_ID;
    }

    @Override
    public <T> Activator<T> activator(InjectionServices injectionServices, Descriptor<T> descriptor) {
        return InjectServiceProvider.create(injectionServices, descriptor);
    }

    @Override
    public double weight() {
        // less than default, so others can override it
        return Weighted.DEFAULT_WEIGHT - 10;
    }
}
