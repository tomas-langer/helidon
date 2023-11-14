package io.helidon.inject.runtime;

import java.util.List;
import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.inject.api.Qualifier;
import io.helidon.inject.api.ServiceDependencies;
import io.helidon.inject.api.ServiceDescriptor;
import io.helidon.inject.api.ServiceProvider;

public abstract class DescribedServiceProvider<T> implements ServiceProvider<T> {
    private final ServiceDescriptor<T> descriptor;

    protected DescribedServiceProvider(ServiceDescriptor<T> descriptor) {
        this.descriptor = descriptor;
    }

    @Override
    public ServiceDescriptor<T> descriptor() {
        return descriptor;
    }

    @Override
    public String runtimeId() {
        return descriptor.runtimeId();
    }

    @Override
    public TypeName serviceType() {
        return descriptor.serviceType();
    }

    @Override
    public Set<TypeName> contracts() {
        return descriptor.contracts();
    }

    @Override
    public List<ServiceDependencies> dependencies() {
        return descriptor.dependencies();
    }

    @Override
    public Set<Qualifier> qualifiers() {
        return descriptor.qualifiers();
    }

    @Override
    public int runLevel() {
        return descriptor.runLevel();
    }

    @Override
    public Set<TypeName> scopes() {
        return descriptor.scopes();
    }

    @Override
    public double weight() {
        return descriptor.weight();
    }
}
