package io.helidon.inject.configdriven.runtime;

import io.helidon.inject.api.ModuleComponent;
import io.helidon.inject.api.ServiceBinder;

/**
 * An explicit and manually implemented module component, as we are doing a bit of unusual work with config bean registry.
 */
public class ConfigDrivenInjectModule implements ModuleComponent {
    /**
     * Constructor for ServiceLoader.
     *
     * @deprecated for use by Java ServiceLoader, do not use directly
     */
    @Deprecated
    public ConfigDrivenInjectModule() {
        super();
    }

    @Override
    public String name() {
        return "io.helidon.inject.configdriven.runtime";
    }

    @Override
    public void configure(ServiceBinder binder) {
        binder.bind(CbrServiceDescriptor.INSTANCE);
    }
}
