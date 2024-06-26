/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

package io.helidon.webserver;

import io.helidon.service.registry.Service;

/**
 * Contract implemented by Helidon to support setting up webserver when using service registry.
 * Helidon will look up all instances of this service and invoke them, so it can be used for customization
 * of webserver configuration.
 */
@Service.Contract
public interface WebServerRegistryService {
    /**
     * Update the builder with services discovered from the service registry.
     *
     * @param builder web server builder
     */
    void updateBuilder(WebServerConfig.BuilderBase<?, ?> builder);
}