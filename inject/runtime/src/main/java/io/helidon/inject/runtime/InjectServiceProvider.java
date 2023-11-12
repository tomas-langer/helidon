package io.helidon.inject.runtime;

import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.ServiceDescriptor;
import io.helidon.inject.api.ServiceInfo;

class InjectServiceProvider<T> extends ServiceProviderBase<T, InjectServiceProvider<T>, InjectServiceActivator<T>> {
    InjectServiceProvider(InjectionServices injectionServices, ServiceDescriptor<T> descriptor) {
        super(injectionServices,
              descriptor,
              new InjectServiceActivator<>(injectionServices, descriptor),
              ServiceInfo.builder()
                      .update(it -> descriptor.contracts().forEach(it::addContractImplemented))
                      .scopeTypeNames(descriptor.scopes())
                      .qualifiers(descriptor.qualifiers())
                      .declaredRunLevel(descriptor.runLevel())
                      .declaredWeight(descriptor.weight())
                      .serviceTypeName(descriptor.serviceType())
                      .build());
    }

    static <T> InjectServiceProvider<T> create(InjectionServices injectionServices, ServiceDescriptor<T> descriptor) {
        return new InjectServiceProvider<>(injectionServices, descriptor);
    }
}
