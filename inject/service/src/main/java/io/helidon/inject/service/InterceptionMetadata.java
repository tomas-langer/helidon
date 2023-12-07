package io.helidon.inject.service;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypedElementInfo;

/**
 * Provides a service descriptor, or an intercepted instance with information
 * whether to, and how to intercept elements.
 */
public interface InterceptionMetadata {
    /**
     * Interceptors that are to be used for the element. The list may be empty
     *
     * @param typeQualifiers  qualifiers of the type (class)
     * @param typeAnnotations annotations on the type (class)
     * @param element         to be intercepted
     * @return a list of interceptors to invoke before reaching the real element
     */
    List<Supplier<Interceptor>> interceptors(Set<Qualifier> typeQualifiers,
                                             List<Annotation> typeAnnotations,
                                             TypedElementInfo element);

    <T> Invoker<T> createInvoker(ServiceInfo<?> descriptor,
                                 Set<Qualifier> typeQualifiers,
                                 List<Annotation> typeAnnotations,
                                 TypedElementInfo element,
                                 Invoker<T> targetInvoker,
                                 Set<Class<? extends Throwable>> checkedExceptions);

    <V> V invoke(ServiceInfo<?> descriptor,
                 List<Annotation> typeAnnotations,
                 TypedElementInfo element,
                 List<Supplier<Interceptor>> interceptors,
                 Invoker<V> call,
                 Object... args);
}
