/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    /**
     * Create an invoker that handles interception if needed.
     *
     * @param descriptor        descriptor of the service being intercepted
     * @param typeQualifiers    qualifiers on the type
     * @param typeAnnotations   annotations on the type
     * @param element           element being intercepted
     * @param targetInvoker     invoker of the element
     * @param checkedExceptions expected checked exceptions that can be thrown by the invoker
     * @param <T>               type of the result of the invoker
     * @return an invoker that handles interception if enabled and if there are matching interceptors
     */
    <T> Invoker<T> createInvoker(ServiceInfo<?> descriptor,
                                 Set<Qualifier> typeQualifiers,
                                 List<Annotation> typeAnnotations,
                                 TypedElementInfo element,
                                 Invoker<T> targetInvoker,
                                 Set<Class<? extends Throwable>> checkedExceptions);

    /**
     * Create an intercepted invoker and invoke it.
     *
     * @param descriptor      descriptor of the service being intercepted
     * @param typeAnnotations annotations on the type
     * @param element         element being intercepted
     * @param interceptors    list of interceptors that match the element
     * @param targetInvoker   invoker of the element
     * @param args            arguments to pass to the first interceptor
     * @param <T>             type of the result of the invoker
     * @return result of the intercepted call
     */
    <T> T invoke(ServiceInfo<?> descriptor,
                 List<Annotation> typeAnnotations,
                 TypedElementInfo element,
                 List<Supplier<Interceptor>> interceptors,
                 Invoker<T> targetInvoker,
                 Object... args);
}
