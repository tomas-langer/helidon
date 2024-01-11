/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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

import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypedElementInfo;

/**
 * Provides a service descriptor, or an intercepted instance with information
 * whether to, and how to intercept elements.
 * <p>
 * Used by generated code (passed as a parameter to
 * {@link io.helidon.inject.service.ServiceDescriptor#inject(InjectionContext, InterceptionMetadata, java.util.Set, Object)}, and
 * {@link io.helidon.inject.service.ServiceDescriptor#instantiate(InjectionContext, InterceptionMetadata)}).
 */
public interface InterceptionMetadata {
    /**
     * Create an invoker that handles interception if needed.
     *
     * @param descriptor        metadata of the service being intercepted
     * @param typeQualifiers    qualifiers on the type
     * @param typeAnnotations   annotations on the type
     * @param element           element being intercepted
     * @param targetInvoker     invoker of the element
     * @param checkedExceptions expected checked exceptions that can be thrown by the invoker
     * @param <T>               type of the result of the invoker
     * @return an invoker that handles interception if enabled and if there are matching interceptors, any checkedException will
     *         be re-thrown, any runtime exception will be re-thrown
     */
    <T> Invoker<T> createInvoker(ServiceInfo descriptor,
                                 Set<Qualifier> typeQualifiers,
                                 List<Annotation> typeAnnotations,
                                 TypedElementInfo element,
                                 Invoker<T> targetInvoker,
                                 Set<Class<? extends Throwable>> checkedExceptions);
}