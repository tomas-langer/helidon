package io.helidon.inject.api;

import java.util.List;
import java.util.Set;

import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypedElementInfo;

import jakarta.inject.Provider;

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
    List<Provider<Interceptor>> interceptors(Set<Qualifier> typeQualifiers,
                                             List<Annotation> typeAnnotations,
                                             TypedElementInfo element);

    <T> Invoker<T> createInvoker(ServiceDescriptor<?> descriptor,
                          Set<Qualifier> typeQualifiers,
                          List<Annotation> typeAnnotations,
                          TypedElementInfo element,
                          Invoker<T> targetInvoker);
}
