package io.helidon.inject.runtime;

import io.helidon.inject.api.ServiceDescriptor;
import io.helidon.inject.api.ServiceProvider;

public abstract class DescribedServiceProvider<T> implements ServiceProvider<T> {
    private final ServiceDescriptor<T> descriptor;

    protected DescribedServiceProvider(ServiceDescriptor<T> descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public ServiceDescriptor<T> descriptor() {
        return descriptor;
    }
}
