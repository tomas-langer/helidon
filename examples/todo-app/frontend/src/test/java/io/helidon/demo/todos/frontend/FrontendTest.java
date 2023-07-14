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

package io.helidon.demo.todos.frontend;

import java.util.Base64;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.nima.testing.junit5.webserver.ServerTest;
import io.helidon.nima.testing.junit5.webserver.SetUpServer;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1ClientResponse;
import io.helidon.nima.webserver.WebServerConfig;
import io.helidon.nima.webserver.http.HttpRoute;
import io.helidon.nima.webserver.http.HttpRules;
import io.helidon.nima.webserver.http.HttpService;
import io.helidon.security.Security;
import io.helidon.security.integration.nima.SecurityFeature;
import io.helidon.tracing.Tracer;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static io.helidon.config.ConfigSources.classpath;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
public class FrontendTest {

    private static final JsonObject TODO = Json.createObjectBuilder().add("msg", "todo").build();
    private static final String ENCODED_ID = Base64.getEncoder().encodeToString("john:password".getBytes());

    private final Http1Client client;

    FrontendTest(Http1Client client) {
        this.client = client;
    }

    @SetUpServer
    static void setup(WebServerConfig.Builder server) {
        Http1Client client = Http1Client.builder().baseUri("http://localhost:8081").build();
        BackendServiceClient bsc = new BackendServiceClient(client);
        Config config = Config.create(classpath("frontend-application.yaml"));
        Security security = Security.create(config.get("security"));

        server.putSocket("@default", socket -> socket
                        .from(server.sockets().get("@default"))
                        .routing(routing -> routing
                                .addFeature(SecurityFeature.create(security, config.get("security")))
                                .register("/env", new EnvHandler(config))
                                .register("/api", new TodosHandler(bsc, Tracer.noOp()))))
                .putSocket("backend", socket -> socket
                        .port(8081)
                        .routing(routing -> routing
                                .register("/api/backend", new FakeBackendService())));
    }

    public static class FakeBackendService implements HttpService {

        @Override
        public void routing(HttpRules rules) {
            rules.get((req, res) -> res.send(Json.createArrayBuilder().add(TODO).build()))
                    .post((req, res) -> res.send(req.content().as(JsonObject.class)))
                    .route(HttpRoute.builder()
                            .methods(Http.Method.GET, Http.Method.DELETE, Http.Method.PUT)
                            .path("/{id}")
                            .handler((req, res) -> res.send(TODO)));
        }
    }

    @Test
    public void testGetList() {
        JsonArray jsonValues = client.get()
                .path("/api/todo")
                .headers(headers -> {
                    headers.add(Http.Header.AUTHORIZATION, "Basic " + ENCODED_ID);
                    return headers;
                })
                .request(JsonArray.class);
        assertThat(jsonValues.getJsonObject(0), is(TODO));
    }

    @Test
    public void testPostTodo() {
        try (Http1ClientResponse response = client.post()
                .path("/api/todo")
                .headers(headers -> {
                    headers.add(Http.Header.AUTHORIZATION, "Basic " + ENCODED_ID);
                    return headers;
                })
                .submit(TODO)) {

            assertThat(response.as(JsonObject.class), is(TODO));
        }
    }

    @Test
    public void testGetTodo() {
        JsonObject jsonObject = client.get()
                .path("/api/todo/1")
                .header(Http.Header.AUTHORIZATION, "Basic " + ENCODED_ID)
                .request(JsonObject.class);

        assertThat(jsonObject, is(TODO));
    }

    @Test
    public void testDeleteTodo() {
        JsonObject jsonObject = client.delete()
                .path("/api/todo/1")
                .header(Http.Header.AUTHORIZATION, "Basic " + ENCODED_ID)
                .request(JsonObject.class);

        assertThat(jsonObject, is(TODO));
    }

    @Test
    public void testUpdateTodo() {
        try (Http1ClientResponse response = client.put()
                .path("/api/todo/1")
                .header(Http.Header.AUTHORIZATION, "Basic " + ENCODED_ID)
                .submit(TODO)) {

            assertThat(response.as(JsonObject.class), is(TODO));
        }
    }

    @Test
    public void testEnvHandler() {
        String env = client.get().path("/env").request(String.class);
        assertThat(env, is("docker"));
    }

}
