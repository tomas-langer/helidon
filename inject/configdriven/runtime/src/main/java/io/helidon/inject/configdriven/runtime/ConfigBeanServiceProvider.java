package io.helidon.inject.configdriven.runtime;

import java.util.Optional;

import io.helidon.inject.api.Activator;
import io.helidon.inject.api.ContextualServiceQuery;
import io.helidon.inject.api.DeActivator;
import io.helidon.inject.api.DependenciesInfo;
import io.helidon.inject.api.Phase;
import io.helidon.inject.api.PostConstructMethod;
import io.helidon.inject.api.PreDestroyMethod;
import io.helidon.inject.api.Qualifier;
import io.helidon.inject.api.ServiceInfo;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.ServiceProviderBindable;

class ConfigBeanServiceProvider<CB> implements ServiceProvider<CB> {
    private final Class<?> beanType;
    private final CB instance;
    private final String id;

    ConfigBeanServiceProvider(Class<?> beanType, CB instance, String id) {
        this.beanType = beanType;
        this.instance = instance;
        this.id = id;
    }

    @Override
    public Optional<CB> first(ContextualServiceQuery query) {
        return Optional.of(instance);
    }

    @Override
    public String id() {
        return beanType.getName() + "{" + id + "}";
    }

    @Override
    public String description() {
        return id() + ":ACTIVE";
    }

    @Override
    public boolean isProvider() {
        return false;
    }

    @Override
    public ServiceInfo serviceInfo() {
        return ServiceInfo.builder()
                .addContractImplemented(beanType)
                .addQualifier(Qualifier.createNamed(id))
                .serviceTypeName(beanType)
                .build();
    }

    @Override
    public DependenciesInfo dependencies() {
        return DependenciesInfo.create();
    }

    @Override
    public Phase currentActivationPhase() {
        return Phase.ACTIVE;
    }

    @Override
    public Optional<Activator> activator() {
        return Optional.empty();
    }

    @Override
    public Optional<DeActivator> deActivator() {
        return Optional.empty();
    }

    @Override
    public Optional<PostConstructMethod> postConstructMethod() {
        return Optional.empty();
    }

    @Override
    public Optional<PreDestroyMethod> preDestroyMethod() {
        return Optional.empty();
    }

    @Override
    public Optional<ServiceProviderBindable<CB>> serviceProviderBindable() {
        return Optional.empty();
    }

    @Override
    public Class<?> serviceType() {
        return beanType;
    }
}
