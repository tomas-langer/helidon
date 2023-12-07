package io.helidon.inject.spi;

import io.helidon.inject.api.Activator;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.service.Descriptor;

public interface ActivatorProvider {
    String id();

    <T> Activator<T> activator(InjectionServices injectionServices, Descriptor<T> descriptor);
}
