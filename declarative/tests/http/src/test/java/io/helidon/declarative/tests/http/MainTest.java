package io.helidon.declarative.tests.http;

import java.util.Map;

import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.testing.junit5.ServerTest;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
class MainTest {
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Map.of());

    private final Http1Client client;

    protected MainTest(Http1Client client) {
        this.client = client;
    }

    @Test
    void testRootRoute() {
        var response = client.get("/greet").request(JsonObject.class);

        assertThat(response.status(), is(Status.OK_200));
        JsonObject json = response.entity();
        assertThat(json.getString("message"), is("Hello World!"));
    }

    @Test
    void testHealthObserver() {
        var response = client.get("/observe/health").request(String.class);
        assertThat(response.status(), is(Status.NO_CONTENT_204));
    }

    @Test
    void testDeadlockHealthCheck() {
        var response = client.get("/observe/health/live/deadlock").request(String.class);
        assertThat(response.status(), is(Status.NO_CONTENT_204));
    }

    @Test
    void testMetricsObserver() {
        var response = client.get("/observe/metrics").request(String.class);
        assertThat(response.status(), is(Status.OK_200));
    }

    @Test
    void testErrorHandler() {
        JsonObject badEntity = JSON.createObjectBuilder().build();

        var response = client.put("/greet/greeting").submit(badEntity, JsonObject.class);
        assertThat(response.status(), is(Status.BAD_REQUEST_400));
        JsonObject entity = response.entity();
        assertThat(entity.getString("error"), is("No greeting provided"));
    }
}
