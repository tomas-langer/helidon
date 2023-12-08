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

import io.helidon.inject.service.IpId;
import io.helidon.inject.service.ServiceInfo;

/**
 * Responsible for registering the injection plan to the services in the service registry.
 */
public interface ServiceInjectionPlanBinder {

    /**
     * Bind an injection plan to a service provider instance.
     *
     * @param serviceDescriptor the service to receive the injection plan.
     * @return the binder to use for binding the injection plan to the service provider
     */
    Binder bindTo(ServiceInfo<?> serviceDescriptor);

    /**
     * Bind all discovered interceptors.
     *
     * @param descriptors interceptor services
     */
    void interceptors(ServiceInfo<?>... descriptors);

    /**
     * The binder builder for the service plan.
     *
     * @see io.helidon.inject.service.IpId
     */
    interface Binder {

        /**
         * Binds a single service provider to the injection point identified by the id.
         * It is assumed that the caller of this is aware of the proper cardinality for each injection point.
         *
         * @param id          the injection point identity
         * @param useProvider whether we inject a provider or provided
         * @param descriptor  the service provider to bind to this identity.
         * @return the binder builder
         */
        Binder bind(IpId id,
                    boolean useProvider,
                    ServiceInfo<?> descriptor);

        /**
         * Bind to an optional field, with zero or one descriptors.
         *
         * @param id          injection point identity
         * @param useProvider whether we inject a provider or provided
         * @param descriptor  the descriptor to bind (zero or one)
         * @return the binder builder
         */
        Binder bindOptional(IpId id,
                            boolean useProvider,
                            ServiceInfo<?>... descriptor);

        /**
         * Binds a list of service providers to the injection point identified by the id.
         * It is assumed that the caller of this is aware of the proper cardinality for each injection point.
         *
         * @param id               the injection point identity
         * @param useProvider      whether we inject a provider or provided
         * @param serviceProviders service descriptors to bind to this identity (zero or more)
         * @return the binder builder
         */
        Binder bindMany(IpId id,
                        boolean useProvider,
                        ServiceInfo<?>... serviceProviders);

        /**
         * Represents a null bind.
         * Binding of null values must be allowed in the registry, by default this is not an option.
         *
         * @param id the injection point identity
         * @return the binder builder
         */
        Binder bindNull(IpId id);

        /**
         * Represents injection points that cannot be bound at startup, and instead must rely on a
         * deferred resolver based binding. Typically, this represents some form of dynamic or configurable instance.
         *
         * @param id          the injection point identity
         * @param useProvider whether to inject provider or service instance
         * @param serviceType the service type needing to be resolved
         * @return the binder builder
         */
        Binder runtimeBind(IpId id,
                           boolean useProvider,
                           Class<?> serviceType);

        /**
         * Bind an {@link java.util.Optional} injection point at runtime.
         *
         * @param id          injection point id
         * @param useProvider whether to inject provider or service instance
         * @param serviceType type of service to be discovered at runtime
         * @return the binder builder
         */
        Binder runtimeBindOptional(IpId id,
                                   boolean useProvider,
                                   Class<?> serviceType);

        /**
         * Bind a {@link java.util.List} injection point at runtime.
         *
         * @param id          injection point id
         * @param useProvider whether to inject provider or service instance
         * @param serviceType type of service to be discovered at runtime
         * @return the binder builder
         */
        Binder runtimeBindMany(IpId id,
                               boolean useProvider,
                               Class<?> serviceType);

        /**
         * Bind a nullable injection point at runtime.
         *
         * @param id          injection point id
         * @param useProvider whether to inject provider or service instance
         * @param serviceType type of service to be discovered at runtime
         * @return the binder builder
         */
        Binder runtimeBindNullable(IpId id,
                                   boolean useProvider,
                                   Class<?> serviceType);

        /**
         * Commits the bindings for this service provider.
         */
        void commit();

    }

}
