package io.helidon.inject.runtime;

import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.service.ServiceInfo;

/**
 * A service provider based on a descriptor.
 * The descriptor will be used to obtain all values inherited from {@link io.helidon.inject.service.ServiceInfo}.
 *
 * @param <T> type of the provided service
 */
public abstract class DescribedServiceProvider<T> implements ServiceProvider<T> {
    private final ServiceInfo<T> serviceInfo;

    /**
     * Creates a new instance with the delegate descriptor.
     *
     * @param serviceInfo descriptor to delegate to
     */
    protected DescribedServiceProvider(ServiceInfo<T> serviceInfo) {
        this.serviceInfo = serviceInfo;
    }

    @Override
    public ServiceInfo<T> serviceInfo() {
        return serviceInfo;
    }
}
