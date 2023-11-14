/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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

package io.helidon.inject.api;

import java.util.Optional;

import io.helidon.common.Weighted;
import io.helidon.common.types.TypeName;

import jakarta.inject.Singleton;

/**
 * Provides management lifecycle around services.
 *
 * @param <T> the type that this service provider manages
 */
public interface ServiceProvider<T> extends ServiceDescriptor<T>, InjectionPointProvider<T>, Weighted {

    /**
     * Identifies the service provider physically and uniquely.
     *
     * @return the unique identity of the service provider
     */
    default String id() {
        return descriptor().serviceType().fqName();
    }

    /**
     * Describe the service provider. This will change based upon activation state.
     *
     * @return the logical and immutable description
     */
    default String description() {
        return descriptor().serviceType().className() + "[" + currentActivationPhase() + "]";
    }

    /**
     * Does the service provide singletons, does it always produce the same result for every call to {@link #get()}.
     * I.e., if the managed service implements Provider or
     * {@link InjectionPointProvider} then this typically is considered not a singleton provider.
     * I.e., If the managed services is NOT {@link Singleton}, then it will be treated as per request / dependent
     * scope.
     * Note that this is similar in nature to RequestScope, except the "official" request scope is bound to the
     * web request. Here, we are speaking about contextually any caller asking for a new instance of the service in
     * question. The requester in question will ideally be able to identify itself to this provider via
     * {@link InjectionPointProvider#first(ContextualServiceQuery)} so that this provider can properly
     * service the "provide" request.
     *
     * @return true if the service provider provides per-request instances for each caller
     */
    default boolean isProvider() {
        return false;
    }

    /**
     * Service descriptor. The type is expected to be generated at compile time, and contains only statically known information.
     * As a result, methods on this type may provide different results than methods on the descriptor returned by this method,
     * as this type honors runtime state.
     *
     * @return descriptor of this service
     */
    ServiceDescriptor<T> descriptor();

    /**
     * The current activation phase for this service provider.
     *
     * @return the activation phase
     */
    Phase currentActivationPhase();

    /**
     * The agent responsible for activation - this will be non-null for build-time activators. If not present then
     * an {@link Injector} must be used to reflectively activate.
     *
     * @return the activator
     */
    default Optional<Activator> activator() {
        return Optional.empty();
    }

    /**
     * The agent responsible for deactivation - this will be non-null for build-time activators. If not present then
     * an {@link Injector} must be used to reflectively deactivate.
     *
     * @return the deactivator to use or null if the service is not interested in deactivation
     */
    default Optional<DeActivator> deActivator() {
        return Optional.empty();
    }

    /**
     * The agent/instance to be used for binding this service provider to the injectable application that was code generated.
     *
     * @return the service provider that should be used for binding, or empty if this provider does not support binding
     * @see ModuleComponent
     * @see ServiceBinder
     * @see ServiceProviderBindable
     */
    default Optional<ServiceProviderBindable<T>> serviceProviderBindable() {
        return Optional.empty();
    }

    @Override
    default TypeName serviceType() {
        return descriptor().serviceType();
    }

    @Override
    default T get() {
        return InjectionPointProvider.super.get();
    }
}
