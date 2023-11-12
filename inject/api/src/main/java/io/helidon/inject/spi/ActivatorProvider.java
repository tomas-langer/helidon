package io.helidon.inject.spi;

import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.ServiceDescriptor;
import io.helidon.inject.api.ServiceProvider;

public interface ActivatorProvider {
    String id();

    <T> ServiceProvider<T> activator(InjectionServices injectionServices, ServiceDescriptor<T> descriptor);
}
