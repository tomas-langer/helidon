package io.helidon.inject;

import java.util.Set;
import java.util.function.Supplier;

import io.helidon.common.types.TypeName;
import io.helidon.inject.service.Lookup;
import io.helidon.inject.service.QualifiedInstance;
import io.helidon.inject.service.Qualifier;
import io.helidon.inject.service.RegistryInstance;
import io.helidon.inject.service.ServiceDescriptor;

final class ServiceManager<T> {
    private final ServiceProvider<T> provider;
    private final Supplier<ManagedService<T>> managedServiceSupplier;
    private final Supplier<Scope> scopeSupplier;

    ServiceManager(Supplier<Scope> scopeSupplier,
                   ServiceProvider<T> provider,
                   Supplier<ManagedService<T>> managedServiceSupplier) {
        this.scopeSupplier = scopeSupplier;
        this.provider = provider;
        this.managedServiceSupplier = managedServiceSupplier;
    }

    public RegistryInstance<T> registryInstance(Lookup lookup, QualifiedInstance<T> instance) {
        return new RegistryInstanceHolder<>(provider.descriptor(),
                                            provider.contracts(lookup),
                                            instance);
    }

    public ServiceInjectionPlanBinder.Binder servicePlanBinder() {
        return provider.servicePlanBinder();
    }

    void activate() {
        managedServiceInScope()
                .activate(provider.activationRequest());
    }

    // creates a new instance on each call!!!!
    ManagedService<T> supplyManagedService() {
        return managedServiceSupplier.get();
    }

    /*
      Get service provider for the scope it is in (always works for singleton, may fail for other)
    */
    ManagedService<T> managedServiceInScope() {
        return scopeSupplier
                .get()
                .services()
                .serviceProvider(this);
    }

    ServiceDescriptor<T> descriptor() {
        return provider.descriptor();
    }

    @Override
    public String toString() {
        return descriptor().serviceType().classNameWithEnclosingNames();
    }

    private static final class RegistryInstanceHolder<T> implements RegistryInstance<T> {
        private final ServiceDescriptor<T> descriptor;
        private final QualifiedInstance<T> qualifiedInstance;
        private final Set<TypeName> contracts;

        private RegistryInstanceHolder(ServiceDescriptor<T> descriptor,
                                       Set<TypeName> contracts,
                                       QualifiedInstance<T> qualifiedInstance) {
            this.descriptor = descriptor;
            this.contracts = contracts;
            this.qualifiedInstance = qualifiedInstance;
        }

        @Override
        public T get() {
            return qualifiedInstance.instance();
        }

        @Override
        public Set<Qualifier> qualifiers() {
            return qualifiedInstance.qualifiers();
        }

        @Override
        public Set<TypeName> contracts() {
            return contracts;
        }

        @Override
        public TypeName scope() {
            return descriptor.scope();
        }

        @Override
        public double weight() {
            return descriptor.weight();
        }

        @Override
        public TypeName serviceType() {
            return descriptor.serviceType();
        }

        @Override
        public String toString() {
            return "Instance of " + descriptor.serviceType().fqName() + ": " + qualifiedInstance;
        }
    }
}
