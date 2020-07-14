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

import io.helidon.config.Config;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MetricsTest {
    @Test
    void testBaseRegistry() {
        MetricRegistry base = Metrics.base();
        assertThat(base, notNullValue());
        Counter counter = base.counter("test");
        assertThat(counter, notNullValue());
    }

    @Test
    void testVendorRegistry() {
        MetricRegistry vendor = Metrics.vendor();
        assertThat(vendor, notNullValue());
        Counter counter = vendor.counter("test");
        assertThat(counter, notNullValue());

    }

    @Test
    void testApplicationRegistry() {
        MetricRegistry app = Metrics.application();
        assertThat(app, notNullValue());
        Counter counter = app.counter("test");
        assertThat(counter, notNullValue());
    }

    @Test
    void testCannotReconfigure() {
        // ensure registry is created
        MetricRegistry registry = Metrics.application();

        assertThrows(IllegalStateException.class, () -> Metrics.config(Config.empty()));
    }

    @Test
    void testMetricIdFails() {
        MetricRegistry registry = Metrics.application();
        assertThrows(NoClassDefFoundError.class, () -> registry.remove(new MetricID("test")));
    }
}