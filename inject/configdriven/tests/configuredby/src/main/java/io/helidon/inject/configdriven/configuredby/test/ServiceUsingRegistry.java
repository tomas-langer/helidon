package io.helidon.inject.configdriven.configuredby.test;

import io.helidon.inject.configdriven.runtime.ConfigBeanRegistry;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
class ServiceUsingRegistry {
    private final ConfigBeanRegistry registry;

    @Inject
    ServiceUsingRegistry(ConfigBeanRegistry registry) {
        this.registry = registry;
    }

    ConfigBeanRegistry registry() {
        return registry;
    }
}
