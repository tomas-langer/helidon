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

/**
 * Responsible for binding service providers to the service registry.
 */
public interface ServiceBinder {

    /**
     * Bind a service provider instance into the backing {@link Services} service registry.
     *
     * @param serviceActivator the service activator for a service provider to bind into the service registry
     */
    void bind(Activator<?> serviceActivator);

    /**
     * Bind a service descriptor instance into the registry.
     *
     * @param serviceDescriptor the descriptor to bind into the service registry
     */
    void bind(ServiceSource<?> serviceDescriptor);
}
