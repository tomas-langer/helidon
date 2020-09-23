/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
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

package io.helidon.security.integration.webserver;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import io.helidon.common.http.Http;
import io.helidon.security.AuditEvent;
import io.helidon.security.Security;
import io.helidon.security.providers.httpauth.HttpBasicAuthProvider;
import io.helidon.webclient.WebClient;
import io.helidon.webclient.WebClientResponse;
import io.helidon.webclient.security.WebClientSecurity;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.junit5.HelidonReactiveTest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * A set of tests that are used both by configuration based
 * and programmatic tests.
 */
@HelidonReactiveTest
public abstract class WebSecurityTests {
    static final String AUDIT_MESSAGE_FORMAT = "Unit test message format";
    static UnitTestAuditProvider myAuditProvider;

    private final WebClient securitySetup;
    private final WebClient webClient;

    WebSecurityTests(WebServer server, WebClient webClient) {
        this.webClient = webClient;

        WebSecurityTestUtil.auditLogFinest();

        Security clientSecurity = Security.builder()
                .addProvider(HttpBasicAuthProvider.builder().build())
                .build();

        this.securitySetup = WebClient.builder()
                .baseUri("http://localhost:" + server.port())
                .addService(WebClientSecurity.create(clientSecurity))
                .build();
    }

    @Test
    void basicTestJohn() throws ExecutionException, InterruptedException {
        String username = "john";
        String password = "password";

        testProtected("/noRoles", username, password, Set.of(), Set.of());
        // this user has no roles, all requests should fail except for public
        testForbidden("/user", username, password);
        testForbidden("/admin", username, password);
        testForbidden("/deny", username, password);
    }

    @Test
    void basicTestJack() throws ExecutionException, InterruptedException {
        String username = "jack";
        String password = "jackIsGreat";

        testProtected("/noRoles",
                      username,
                      password,
                      Set.of("user", "admin"),
                      Set.of());
        testProtected("/user",
                      username,
                      password,
                      Set.of("user", "admin"),
                      Set.of());
        testProtected("/admin",
                      username,
                      password,
                      Set.of("user", "admin"),
                      Set.of());
        testForbidden("/deny", username, password);
    }

    @Test
    void basicTestJill() throws ExecutionException, InterruptedException {
        String username = "jill";
        String password = "password";

        testProtected("/noRoles",
                      username,
                      password,
                      Set.of("user"),
                      Set.of("admin"));
        testProtected("/user",
                      username,
                      password,
                      Set.of("user"),
                      Set.of("admin"));
        testForbidden("/admin", username, password);
        testForbidden("/deny", username, password);
    }

    @Test
    void basicTest401() throws ExecutionException, InterruptedException {
        webClient.get()
                .path("/noRoles")
                .request()
                .thenAccept(it -> {
                    assertThat(it.status(), is(Http.Status.UNAUTHORIZED_401));
                    it.headers()
                            .first(Http.Header.WWW_AUTHENTICATE)
                            .ifPresentOrElse(header -> assertThat(header.toLowerCase(), is("basic realm=\"mic\"")),
                                             () -> {
                                                 throw new IllegalStateException("Header " + Http.Header.WWW_AUTHENTICATE + " is"
                                                                                         + " not present in response!");
                                             });
                })
                .toCompletableFuture()
                .get();

        WebClientResponse webClientResponse = callProtected("/noRoles", "invalidUser", "invalidPassword");
        assertThat(webClientResponse.status(), is(Http.Status.UNAUTHORIZED_401));
        webClientResponse.headers()
                .first(Http.Header.WWW_AUTHENTICATE)
                .ifPresentOrElse(header -> assertThat(header.toLowerCase(), is("basic realm=\"mic\"")),
                                 () -> {
                                     throw new IllegalStateException("Header " + Http.Header.WWW_AUTHENTICATE + " is"
                                                                             + " not present in response!");
                                 });
    }

    @Test
    void testCustomizedAudit() throws InterruptedException, ExecutionException {
        webClient.get()
                .path("/auditOnly")
                .request()
                .thenCompose(it -> {
                    assertThat(it.status(), is(Http.Status.OK_200));
                    return it.close();
                })
                .await(10, TimeUnit.SECONDS);

        // audit
        AuditEvent auditEvent = myAuditProvider.getAuditEvent();
        assertThat(auditEvent, notNullValue());
        assertThat(auditEvent.messageFormat(), is(AUDIT_MESSAGE_FORMAT));
        assertThat(auditEvent.severity(), is(AuditEvent.AuditSeverity.SUCCESS));
    }

    private void testForbidden(String uri, String username, String password) throws ExecutionException, InterruptedException {
        WebClientResponse response = callProtected(uri, username, password);
        assertThat(uri + " for user " + username + " should be forbidden",
                   response.status(),
                   is(Http.Status.FORBIDDEN_403));
    }

    private void testProtected(String uri,
                               String username,
                               String password,
                               Set<String> expectedRoles,
                               Set<String> invalidRoles) throws ExecutionException, InterruptedException {

        WebClientResponse response = callProtected(uri, username, password);

        assertThat(response.status(), is(Http.Status.OK_200));

        response.content()
                .as(String.class)
                .thenAccept(it -> {
                    // check login
                    assertThat(it, containsString("id='" + username + "'"));
                    // check roles
                    expectedRoles.forEach(role -> assertThat(it, containsString(":" + role)));
                    invalidRoles.forEach(role -> assertThat(it, not(containsString(":" + role))));
                })
                .toCompletableFuture()
                .get();
    }

    private WebClientResponse callProtected(String path, String username, String password)
            throws ExecutionException, InterruptedException {
        return securitySetup.get()
                .path(path)
                .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_USER, username)
                .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_PASSWORD, password)
                .request()
                .toCompletableFuture()
                .get();
    }

}
