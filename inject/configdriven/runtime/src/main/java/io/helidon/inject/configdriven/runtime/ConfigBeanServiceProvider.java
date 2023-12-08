package io.helidon.inject.configdriven.runtime;

import java.util.Optional;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.ContextualServiceQuery;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.service.ServiceInfo;

class ConfigBeanServiceProvider<CB> implements ServiceProvider<CB> {
    private final ConfigBeanServiceDescriptor<CB> serviceDescriptor;
    private final CB instance;
    private final String id;

    ConfigBeanServiceProvider(TypeName beanType, CB instance, String id) {
        this.instance = instance;
        this.id = id;
        this.serviceDescriptor = new ConfigBeanServiceDescriptor<>(beanType, id);
    }

    @Override
    public Optional<CB> first(ContextualServiceQuery query) {
        return Optional.of(instance);
    }

    @Override
    public String id() {
        return ServiceProvider.super.id() + "{" + id + "}";
    }

    @Override
    public String description() {
        return serviceInfo().serviceType().classNameWithEnclosingNames() + "{" + id + "}:ACTIVE";
    }

    @Override
    public ServiceInfo<CB> serviceInfo() {
        return serviceDescriptor;
    }

    @Override
    public Phase currentActivationPhase() {
        return Phase.ACTIVE;
    }
}
