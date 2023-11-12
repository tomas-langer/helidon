package io.helidon.inject.configdriven.runtime;

import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.ServiceDescriptor;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.spi.ActivatorProvider;

public class ConfigDrivenActivatorProvider implements ActivatorProvider {
    @Override
    public String id() {
        return "CONFIG_DRIVEN";
    }

    @Override
    public <T> ServiceProvider<T> activator(InjectionServices injectionServices, ServiceDescriptor<T> descriptor) {
        return ConfigDrivenServiceProvider.create(injectionServices, descriptor);
    }
}
