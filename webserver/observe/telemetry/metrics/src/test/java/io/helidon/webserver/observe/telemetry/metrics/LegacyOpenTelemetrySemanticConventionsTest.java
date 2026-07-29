/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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

package io.helidon.webserver.observe.telemetry.metrics;

import java.util.List;
import java.util.logging.Logger;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.config.Config;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpServer;

import io.opentelemetry.exporter.logging.otlp.OtlpJsonLoggingMetricExporter;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.stringContainsInOrder;

@ServerTest
class LegacyOpenTelemetrySemanticConventionsTest {
    private final Http1Client client;

    LegacyOpenTelemetrySemanticConventionsTest(Http1Client client) {
        this.client = client;
    }

    @SetUpServer
    static void setupServer(WebServerConfig.Builder serverBuilder) {
        Config config = Config.just("""
                                            server:
                                              features:
                                                observe:
                                                  observers:
                                                    metrics:
                                                      auto-http-metrics:
                                                        use-updated-http-metrics: false
                                            """,
                                    MediaTypes.APPLICATION_YAML);
        serverBuilder.config(config.get("server"))
                .routing(rules -> rules.get("/greet/{name}",
                                            (req, res) -> res.send("Hello, " + req.path().pathParameters().get("name"))));
    }

    @Test
    void legacyMetricsUseWebServerMatchingPattern() {
        try (TestLogHandler testLogHandler = TestLogHandler.create(
                Logger.getLogger(OtlpJsonLoggingMetricExporter.class.getName()));
                Http1ClientResponse response = client.get("/greet/Joe").request()) {
            assertThat(response.status().code(), is(200));

            List<String> metricMessages = testLogHandler.messages(
                    hasItem(stringContainsInOrder(List.of(OpenTelemetryMetricsHttpSemanticConventions.TIMER_NAME,
                                                          "/greet/{name}"))));

            assertThat(metricMessages,
                       hasItem(stringContainsInOrder(List.of(OpenTelemetryMetricsHttpSemanticConventions.TIMER_NAME,
                                                             "/greet/{name}"))));
        }
    }
}
