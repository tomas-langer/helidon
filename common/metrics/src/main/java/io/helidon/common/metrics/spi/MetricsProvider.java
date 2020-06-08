/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

package io.helidon.common.metrics.spi;

import io.helidon.config.Config;

import org.eclipse.microprofile.metrics.MetricRegistry;

public interface MetricsProvider {
    /**
     * Create a metrics registry of the defined type.
     *
     * @param config configuration to use
     * @param registryType type of registry
     * @return a configured metric registry
     */
    MetricRegistry createRegistry(Config config, MetricRegistry.Type registryType);
}
