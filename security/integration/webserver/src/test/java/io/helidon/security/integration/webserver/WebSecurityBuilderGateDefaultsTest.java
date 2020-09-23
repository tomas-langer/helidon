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

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.config.Config;
import io.helidon.security.AuditEvent;
import io.helidon.security.Security;
import io.helidon.security.SecurityContext;
import io.helidon.security.providers.httpauth.HttpBasicAuthProvider;
import io.helidon.webclient.WebClient;
import io.helidon.webclient.WebClientResponse;
import io.helidon.webclient.security.WebClientSecurity;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.junit5.HelidonReactiveTest;
import io.helidon.webserver.junit5.SetupRouting;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test for {@link WebSecurity}.
 */
@HelidonReactiveTest
public class WebSecurityBuilderGateDefaultsTest {
    private static UnitTestAuditProvider myAuditProvider;

    private final WebClient securitySetup;
    private final WebClient webClient;

    WebSecurityBuilderGateDefaultsTest(WebServer server, WebClient webClient) {
        this.webClient = webClient;

        WebSecurityTestUtil.auditLogFinest();

        Security clientSecurity = Security.builder()
                .addProvider(HttpBasicAuthProvider.builder().build())
                .build();

        securitySetup = WebClient.builder()
                .baseUri("http://localhost:" + server.port())
                .addService(WebClientSecurity.create(clientSecurity))
                .build();
    }

    @SetupRouting
    static Routing setupRouting(Config config) {
        myAuditProvider = new UnitTestAuditProvider();

        Security security = Security.builder(config.get("security"))
                .addAuditProvider(myAuditProvider).build();

        return Routing.builder()
                .register(WebSecurity.create(security).securityDefaults(WebSecurity.rolesAllowed("admin").audit()))
                // will only accept admin (due to gate defaults)
                .get("/noRoles", WebSecurity.enforce())
                .get("/user[/{*}]", WebSecurity.rolesAllowed("user"))
                .get("/admin", WebSecurity.rolesAllowed("admin"))
                // will also accept admin (due to gate defaults)
                .get("/deny", WebSecurity.rolesAllowed("deny"))
                // audit is on from gate defaults
                .get("/auditOnly", WebSecurity
                        .enforce()
                        .skipAuthentication()
                        .skipAuthorization()
                        .auditEventType("unit_test")
                        .auditMessageFormat(WebSecurityTests.AUDIT_MESSAGE_FORMAT)
                )
                .get("/{*}", (req, res) -> {
                    Optional<SecurityContext> securityContext = req.context().get(SecurityContext.class);
                    res.headers().contentType(MediaType.TEXT_PLAIN.withCharset("UTF-8"));
                    res.send("Hello, you are: \n" + securityContext
                            .map(ctx -> ctx.user().orElse(SecurityContext.ANONYMOUS).toString())
                            .orElse("Security context is null"));
                })
                .build();
    }

    @Test
    public void basicTestJohn() throws ExecutionException, InterruptedException {
        String username = "john";
        String password = "password";

        testForbidden("/noRoles", username, password);
        testForbidden("/user", username, password);
        testForbidden("/admin", username, password);
        testForbidden("/deny", username, password);
    }

    @Test
    public void basicTestJack() throws ExecutionException, InterruptedException {
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
        testProtected("/deny",
                      username,
                      password,
                      Set.of("user", "admin"),
                      Set.of());
    }

    @Test
    public void basicTestJill() throws ExecutionException, InterruptedException {
        String username = "jill";
        String password = "password";

        testForbidden("/noRoles", username, password);
        testProtected("/user",
                      username,
                      password,
                      Set.of("user"),
                      Set.of("admin"));
        testForbidden("/admin", username, password);
        testForbidden("/deny", username, password);
    }

    @Test
    public void basicTest401() throws ExecutionException, InterruptedException {
        // here we call the endpoint
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
    public void testCustomizedAudit() throws InterruptedException, ExecutionException {
        // even though I send username and password, this MUST NOT require authentication
        // as then audit is called twice - first time with 401 (challenge) and second time with 200 (correct request)
        // and that intermittently breaks this test
        webClient.get()
                .path("/auditOnly")
                .request()
                .thenCompose(it -> {
                    assertThat(it.status(), is(Http.Status.OK_200));
                    return it.close();
                })
                .toCompletableFuture()
                .get();

        // audit
        AuditEvent auditEvent = myAuditProvider.getAuditEvent();
        assertThat(auditEvent, notNullValue());
        assertThat(auditEvent.toString(), auditEvent.messageFormat(), is(WebSecurityTests.AUDIT_MESSAGE_FORMAT));
        assertThat(auditEvent.toString(), auditEvent.severity(), is(AuditEvent.AuditSeverity.SUCCESS));
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
        // here we call the endpoint
        return securitySetup.get()
                .path(path)
                .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_USER, username)
                .property(HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_PASSWORD, password)
                .request()
                .toCompletableFuture()
                .get();
    }
}
