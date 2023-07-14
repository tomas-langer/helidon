/*
 * Copyright (c) 2021, 2023 Oracle and/or its affiliates.
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

package io.helidon.examples.webserver.threadpool;

import java.util.Collections;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.nima.testing.junit5.webserver.ServerTest;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1ClientResponse;
import io.helidon.nima.webserver.WebServerConfig;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
public class MainTest {

    private static final JsonBuilderFactory JSON_BUILDER = Json.createBuilderFactory(Collections.emptyMap());
    private static final JsonObject TEST_JSON_OBJECT = JSON_BUILDER.createObjectBuilder()
            .add("greeting", "Hola")
            .build();

    private final Http1Client client;

    public MainTest(Http1Client client) {
        this.client = client;
    }

    @BeforeAll
    public static void setup(WebServerConfig.Builder server) {
        Config config = Config.builder().addSource(ConfigSources.classpath("application-test.yaml")).build();
        Main.setup(server, config);
    }

    @Test
    public void testHelloWorld() {
        try (Http1ClientResponse response = client.get("/greet").request()) {
            assertThat(response.as(JsonObject.class).getString("message"), is("Hello World!"));
        }

        try (Http1ClientResponse response = client.get("/greet/Joe").request()) {
            assertThat(response.as(JsonObject.class).getString("message"), is("Hello Joe!"));
        }

        try (Http1ClientResponse response = client.put("/greet/greeting").submit(TEST_JSON_OBJECT)) {
            assertThat(response.status().code(), is(204));
        }

        try (Http1ClientResponse response = client.get("/greet/Joe").request()) {
            assertThat(response.as(JsonObject.class).getString("message"), is("Hola Joe!"));
        }

        try (Http1ClientResponse response = client.get("/health").request()) {
            assertThat(response.status().code(), is(200));
        }

        try (Http1ClientResponse response = client.get("/metrics").request()) {
            assertThat(response.status().code(), is(200));
        }
    }

}
