package io.helidon.inject.configdriven.runtime;

import io.helidon.inject.api.Activator;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.ServiceSource;
import io.helidon.inject.spi.ActivatorProvider;

/**
 * {@link java.util.ServiceLoader} service implementation of a custom activator provider used only to initialize
 * Config bean registry.
 */
public class CbrActivatorProvider implements ActivatorProvider {
    public CbrActivatorProvider() {
    }

    @Override
    public String id() {
        return CbrServiceDescriptor.CBR_RUNTIME_ID;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Activator<T> activator(InjectionServices injectionServices, ServiceSource<T> descriptor) {
        if (!(descriptor instanceof CbrServiceDescriptor cbrDescriptor)) {
            throw new IllegalArgumentException("Config Driven Activator only supports descriptors of its own type,"
                                                       + " invalid descriptor provided: "
                                                       + descriptor.descriptorType().fqName());
        }
        return (Activator<T>) new CbrProvider(injectionServices, cbrDescriptor);
    }
}
