package io.helidon.inject.runtime;

import java.util.List;
import java.util.Set;

import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.InterceptionMetadata;
import io.helidon.inject.api.Interceptor;
import io.helidon.inject.api.InvocationContext;
import io.helidon.inject.api.Invoker;
import io.helidon.inject.api.Qualifier;
import io.helidon.inject.api.ServiceDescriptor;

import jakarta.inject.Provider;

class InterceptionMetadataImpl implements InterceptionMetadata {
    private final InjectionServices injectionServices;

    InterceptionMetadataImpl(InjectionServices injectionServices) {
        this.injectionServices = injectionServices;
    }

    @Override
    public List<Provider<Interceptor>> interceptors(Set<Qualifier> typeQualifiers,
                                                    List<Annotation> typeAnnotations,
                                                    TypedElementInfo element) {
        return null;
    }

    @Override
    public <T> Invoker<T> createInvoker(ServiceDescriptor<?> descriptor,
                                        Set<Qualifier> typeQualifiers,
                                        List<Annotation> typeAnnotations,
                                        TypedElementInfo element,
                                        Invoker<T> targetInvoker,
                                        TypeName... checkedExceptions) {
        var interceptors = interceptors(typeQualifiers,
                                        typeAnnotations,
                                        element);
        if (interceptors.isEmpty()) {
            return targetInvoker;
        } else {
            return params -> Invocation.createInvokeAndSupply(InvocationContext.builder()
                                                                      .serviceDescriptor(descriptor)
                                                                      .classAnnotations(typeAnnotations)
                                                                      .elementInfo(element)
                                                                      .interceptors(interceptors)
                                                                      .build(),
                                                              targetInvoker,
                                                              params);
        }
    }
}
