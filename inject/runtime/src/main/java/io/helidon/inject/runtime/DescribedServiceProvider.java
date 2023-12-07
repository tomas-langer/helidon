package io.helidon.inject.runtime;

import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.service.ServiceInfo;

public abstract class DescribedServiceProvider<T> implements ServiceProvider<T> {
    private final ServiceInfo<T> descriptor;

    protected DescribedServiceProvider(ServiceInfo<T> descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public ServiceInfo<T> descriptor() {
        return descriptor;
    }
}
