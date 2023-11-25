package io.helidon.inject.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.inject.api.CommonQualifiers;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.InterceptionMetadata;
import io.helidon.inject.api.Interceptor;
import io.helidon.inject.api.InvocationContext;
import io.helidon.inject.api.Invoker;
import io.helidon.inject.api.Qualifier;
import io.helidon.inject.api.ServiceDescriptor;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.Services;

import jakarta.inject.Provider;

class InterceptionMetadataImpl implements InterceptionMetadata {
    private final Services services;

    InterceptionMetadataImpl(InjectionServices services) {
        this.services = services.services();
    }

    @Override
    public List<Provider<Interceptor>> interceptors(Set<Qualifier> typeQualifiers,
                                                    List<Annotation> typeAnnotations,
                                                    TypedElementInfo element) {
        // need to find all interceptors for the providers (ordered by weight)
        List<ServiceProvider<Interceptor>> allInterceptors = services.lookupAll(Interceptor.class, CommonQualifiers.WILDCARD);

        List<Provider<Interceptor>> result = new ArrayList<>();

        for (ServiceProvider<Interceptor> interceptor : allInterceptors) {
            if (applicable(typeAnnotations, interceptor)) {
                result.add(interceptor);
                continue;
            }
            if (applicable(element.annotations(), interceptor)) {
                result.add(interceptor);
            }
        }

        return result;
    }

    @Override
    public <T> Invoker<T> createInvoker(ServiceDescriptor<?> descriptor,
                                 Set<Qualifier> typeQualifiers,
                                 List<Annotation> typeAnnotations,
                                 TypedElementInfo element,
                                 Invoker<T> targetInvoker,
                                 Set<Class<? extends Throwable>> checkedExceptions) {
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
                                                              params,
                                                              checkedExceptions);
        }
    }

    private boolean applicable(List<Annotation> typeAnnotations, ServiceProvider<Interceptor> interceptor) {
        for (Annotation typeAnnotation : typeAnnotations) {
            if (interceptor.qualifiers().contains(Qualifier.createNamed(typeAnnotation.typeName().fqName()))) {
                return true;
            }
        }
        return false;
    }
}
