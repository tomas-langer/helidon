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

package io.helidon.common.metrics;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.common.LazyValue;
import io.helidon.common.metrics.spi.MetricsProvider;
import io.helidon.common.serviceloader.HelidonServiceLoader;
import io.helidon.config.Config;

import org.eclipse.microprofile.metrics.MetricRegistry;

/**
 * Holder of metrics registries.
 * This class is statically configured and used by all components in Helidon.
 *
 * @see #configure(io.helidon.config.Config)
 */
public class Metrics {
    private static final AtomicReference<Config> CONFIG_REF = new AtomicReference<>();

    private final LazyValue<MetricRegistry> application;
    private final LazyValue<MetricRegistry> base;
    private final LazyValue<MetricRegistry> vendor;

    private Metrics(MetricsProvider provider, Config config) {
        this.application = LazyValue.create(() -> provider.createRegistry(config, MetricRegistry.Type.APPLICATION));
        this.vendor = LazyValue.create(() -> provider.createRegistry(config, MetricRegistry.Type.VENDOR));
        this.base = LazyValue.create(() -> provider.createRegistry(config, MetricRegistry.Type.BASE));
    }

    public static MetricRegistry application() {
        return instance().application.get();
    }

    public static MetricRegistry base() {
        return instance().base.get();
    }

    public static MetricRegistry vendor() {
        return instance().vendor.get();
    }

    private static synchronized Metrics instance() {
        CONFIG_REF.compareAndSet(null, Config.empty());

        MetricsProvider provider = HelidonServiceLoader.builder(ServiceLoader.load(MetricsProvider.class))
                .addService(new NoopMetricsProvider(), Integer.MAX_VALUE)
                .build()
                .asList()
                .get(0);

        return new Metrics(provider, CONFIG_REF.get());
    }

    /**
     * Configure the metrics module using the provided config.
     *
     * @param config on the node of metrics configuration (usually {@code metrics})
     * @throws java.lang.IllegalStateException in case metrics are already initialized (and cannot be reconfigured)
     */
    public static void configure(Config config) {
        if (!CONFIG_REF.compareAndSet(null, config)) {
            throw new IllegalArgumentException("Metrics are already configured, please call this method earlier in the"
                                                       + " application lifecycle");
        }
    }
}
