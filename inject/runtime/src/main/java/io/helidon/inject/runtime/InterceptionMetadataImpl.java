package io.helidon.inject.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.inject.api.InjectionServices;
import io.helidon.inject.api.ServiceProvider;
import io.helidon.inject.api.Services;
import io.helidon.inject.service.Inject;
import io.helidon.inject.service.InterceptionMetadata;
import io.helidon.inject.service.Interceptor;
import io.helidon.inject.service.InvocationContext;
import io.helidon.inject.service.Invoker;
import io.helidon.inject.service.Qualifier;
import io.helidon.inject.service.ServiceInfo;

class InterceptionMetadataImpl implements InterceptionMetadata {
    private final Services services;

    InterceptionMetadataImpl(InjectionServices services) {
        this.services = services.services();
    }

    @Override
    public List<Supplier<Interceptor>> interceptors(Set<Qualifier> typeQualifiers,
                                                    List<Annotation> typeAnnotations,
                                                    TypedElementInfo element) {
        // need to find all interceptors for the providers (ordered by weight)
        List<ServiceProvider<Interceptor>> allInterceptors;
        if (services instanceof DefaultServices ds) {
            allInterceptors = ds.interceptors();
        } else {
            allInterceptors = services.lookupAll(Interceptor.class, Inject.Named.WILDCARD_NAME);
        }

        List<Supplier<Interceptor>> result = new ArrayList<>();

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
    public <T> Invoker<T> createInvoker(ServiceInfo<?> descriptor,
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

    @Override
    public <V> V invoke(ServiceInfo<?> descriptor,
                        List<Annotation> typeAnnotations,
                        TypedElementInfo element,
                        List<Supplier<Interceptor>> interceptors,
                        Invoker<V> targetInvoker,
                        Object... args) {
        return Invocation.createInvokeAndSupply(descriptor,
                                                typeAnnotations,
                                                element,
                                                interceptors,
                                                targetInvoker,
                                                args);
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
