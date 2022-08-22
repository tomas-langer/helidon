/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

package io.helidon.nima.tests.integration.http2.webserver;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import io.helidon.common.configurable.Resource;
import io.helidon.common.http.Http;
import io.helidon.common.http.Http.Header;
import io.helidon.common.http.Http.HeaderValue;
import io.helidon.common.pki.KeyConfig;
import io.helidon.nima.common.tls.Tls;
import io.helidon.nima.http2.webserver.Http2Route;
import io.helidon.nima.testing.junit5.webserver.ServerTest;
import io.helidon.nima.testing.junit5.webserver.SetUpRoute;
import io.helidon.nima.testing.junit5.webserver.SetUpServer;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.http1.Http1ClientResponse;
import io.helidon.nima.webserver.WebServer;
import io.helidon.nima.webserver.http.HttpRouting;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
class Http2ServerTest {
    public static final String MESSAGE = "Hello World!";
    private static final String TEST_HEADER_NAME = "custom_header";
    private static final String TEST_HEADER_VALUE = "as!fd";
    private static final HeaderValue TEST_HEADER = HeaderValue.create(Header.create(TEST_HEADER_NAME), TEST_HEADER_VALUE);
    private final int plainPort;
    private final int tlsPort;
    private final Http1Client http1Client;
    private final Tls insecureTls;

    Http2ServerTest(WebServer server, Http1Client http1Client) {
        this.plainPort = server.port();
        this.tlsPort = server.port("https");
        this.http1Client = http1Client;
        this.insecureTls = Tls.builder()
                // insecure setup, as we have self-signed certificate
                .trustAll(true)
                .build();
    }

    @SetUpServer
    static void setUpServer(WebServer.Builder serverBuilder) {
        KeyConfig privateKeyConfig = KeyConfig.keystoreBuilder()
                .keystore(Resource.create("certificate.p12"))
                .keystorePassphrase("helidon")
                .build();

        Tls tls = Tls.builder()
                .privateKey(privateKeyConfig.privateKey().get())
                .privateKeyCertChain(privateKeyConfig.certChain())
                .build();

        serverBuilder.socket("https",
                             socketBuilder -> socketBuilder.tls(tls));
    }

    @SetUpRoute
    static void router(HttpRouting.Builder router) {
        // explicitly on HTTP/2 only, to make sure we do upgrade
        router.route(Http2Route.route(Http.Method.GET, "/", (req, res) -> res.header(TEST_HEADER)
                .send(MESSAGE)));
    }

    @Test
    void testHttp1() {
        // make sure the HTTP/1 route is not working

        Http1ClientResponse response = http1Client.get("/")
                .request();

        assertThat(response.status(), is(Http.Status.NOT_FOUND_404));
    }

    @Test
    void testUpgrade() throws IOException, InterruptedException {
        HttpResponse<String> response = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(5))
                .build()
                .send(HttpRequest.newBuilder()
                              .timeout(Duration.ofSeconds(5))
                              .uri(URI.create("http://localhost:" + plainPort + "/"))
                              .GET()
                              .build(),
                      HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode(), is(200));
        assertThat(response.body(), is(MESSAGE));
        assertThat(response.headers().firstValue(TEST_HEADER_NAME), is(Optional.of(TEST_HEADER_VALUE)));
    }

    @Test
    void testAppProtocol() throws IOException, InterruptedException {
        HttpResponse<String> response = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(5))
                .sslContext(insecureTls.sslContext())
                .build()
                .send(HttpRequest.newBuilder()
                              .timeout(Duration.ofSeconds(5))
                              .uri(URI.create("https://localhost:" + tlsPort + "/"))
                              .GET()
                              .build(),
                      HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode(), is(200));
        assertThat(response.body(), is(MESSAGE));
        assertThat(response.headers().firstValue(TEST_HEADER_NAME), is(Optional.of(TEST_HEADER_VALUE)));
        System.clearProperty("jdk.internal.httpclient.disableHostnameVerification");
    }

    @Test
    @Disabled("No way to test using JDK HTTP Client, Should be part of HTTP/2 client tests")
    void testPriorKnowledge() {

    }
}
