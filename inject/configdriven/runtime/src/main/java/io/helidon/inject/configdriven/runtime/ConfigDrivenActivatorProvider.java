package io.helidon.inject.configdriven.runtime;

import io.helidon.common.Weighted;
import io.helidon.inject.api.Activator;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.service.Descriptor;
import io.helidon.inject.spi.ActivatorProvider;

/**
 * A {@link java.util.ServiceLoader} provider implementation of an {@link io.helidon.inject.spi.ActivatorProvider} that
 * supports config driven beans.
 */
public class ConfigDrivenActivatorProvider implements ActivatorProvider, Weighted {
    /**
     * Required default constructor.
     *
     * @deprecated required by {@link java.util.ServiceLoader}
     */
    @Deprecated
    public ConfigDrivenActivatorProvider() {
    }

    @Override
    public String id() {
        return "CONFIG_DRIVEN";
    }

    @Override
    public <T> Activator<T> activator(InjectionServices injectionServices, Descriptor<T> descriptor) {
        return ConfigDrivenServiceProvider.create(injectionServices, descriptor);
    }

    @Override
    public double weight() {
        // less than default, so others can override it
        return Weighted.DEFAULT_WEIGHT - 10;
    }
}
