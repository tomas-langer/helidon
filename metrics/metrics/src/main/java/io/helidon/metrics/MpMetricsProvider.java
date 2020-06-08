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

package io.helidon.metrics;

import javax.annotation.Priority;

import io.helidon.common.metrics.spi.MetricsProvider;
import io.helidon.config.Config;

import org.eclipse.microprofile.metrics.MetricRegistry;

@Priority(200)
public class MpMetricsProvider implements MetricsProvider {
    private RegistryFactory factory;

    @Deprecated
    public MpMetricsProvider() {
    }

    @Override
    public MetricRegistry createRegistry(Config config, MetricRegistry.Type registryType) {
        return factory(config).getRegistry(registryType);
    }

    private synchronized RegistryFactory factory(Config config) {
        if (null == factory) {
            factory = RegistryFactory.getInstance(config);
        }
        return factory;
    }
}
