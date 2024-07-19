/*
 * Copyright (c) 2018, 2023 Oracle and/or its affiliates.
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

package io.helidon.examples.quickstart.se;

import java.util.Map;

import io.helidon.http.Status;
import io.helidon.webclient.api.ClientResponseTyped;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.testing.junit5.ServerTest;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest(useRegistry = true)
class MainTest {
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Map.of());

    private final Http1Client client;

    protected MainTest(Http1Client client) {
        this.client = client;
    }

    @Test
    void testRootRoute() {
        try (Http1ClientResponse response = client.get("/greet").request()) {
            assertThat(response.status(), is(Status.OK_200));
            JsonObject json = response.as(JsonObject.class);
            assertThat(json.getString("message"), is("Hello World!"));
        }
    }

    @Test
    void testHealthObserver() {
        try (Http1ClientResponse response = client.get("/observe/health").request()) {
            assertThat(response.status(), is(Status.NO_CONTENT_204));
        }
    }

    @Test
    void testDeadlockHealthCheck() {
        try (Http1ClientResponse response = client.get("/observe/health/live/deadlock").request()) {
            assertThat(response.status(), is(Status.NO_CONTENT_204));
        }
    }

    @Test
    void testMetricsObserver() {
        try (Http1ClientResponse response = client.get("/observe/metrics").request()) {
            assertThat(response.status(), is(Status.OK_200));
        }
    }

    @Test
    void testErrorHandler() {
        JsonObject badEntity = JSON.createObjectBuilder().build();

        ClientResponseTyped<JsonObject> response = client.put("/greet/greeting").submit(badEntity, JsonObject.class);
        assertThat(response.status(), is(Status.BAD_REQUEST_400));
        JsonObject entity = response.entity();
        assertThat(entity.getString("error"), is("No greeting provided"));
    }
}
