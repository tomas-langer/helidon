package io.helidon.inject.spi;

import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceSource;

public interface ActivatorProvider {
    String id();

    <T> ServiceProvider<T> activator(InjectionServices injectionServices, ServiceSource<T> descriptor);
}
