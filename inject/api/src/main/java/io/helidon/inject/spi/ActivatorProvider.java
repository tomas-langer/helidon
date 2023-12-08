package io.helidon.inject.spi;

import io.helidon.inject.api.Activator;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.service.Descriptor;

/**
 * A {@link java.util.ServiceLoader} provider interface that allows extensibility of the activator created based
 * on the service descriptor.
 * The {@link #id()} is matched with {@link io.helidon.inject.service.Descriptor#runtimeId()} and the provider with
 * highest {@link io.helidon.common.Weight} for that id would be used to prepare an activator for the service descriptor.
 */
public interface ActivatorProvider {
    /**
     * Id to match with {@link io.helidon.inject.service.Descriptor#runtimeId()}.
     *
     * @return id this provider provides
     */
    String id();

    /**
     * Create a service activator for a service descriptor.
     *
     * @param injectionServices current injection services
     * @param descriptor        descriptor to create activator for
     * @param <T>               type of the provided service
     * @return a new activator
     */
    <T> Activator<T> activator(InjectionServices injectionServices, Descriptor<T> descriptor);
}
