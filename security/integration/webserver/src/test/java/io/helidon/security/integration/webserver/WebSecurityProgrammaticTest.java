/*
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
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
import java.util.regex.Pattern;

import io.helidon.common.http.Http;
import io.helidon.common.http.MediaType;
import io.helidon.config.Config;
import io.helidon.security.Security;
import io.helidon.security.SecurityContext;
import io.helidon.security.util.TokenHandler;
import io.helidon.webclient.WebClient;
import io.helidon.webserver.Routing;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.junit5.SetupRouting;

/**
 * Unit test for {@link WebSecurity}.
 */
public class WebSecurityProgrammaticTest extends WebSecurityTests {
    WebSecurityProgrammaticTest(WebServer server, WebClient webClient) {
        super(server, webClient);
    }

    @SetupRouting
    public static Routing setupRouting(Config config)  {
        WebSecurityTestUtil.auditLogFinest();
        myAuditProvider = new UnitTestAuditProvider();

        Security security = Security.builder(config.get("security"))
                .addAuditProvider(myAuditProvider).build();

        return Routing.builder()
                .register(WebSecurity.create(security)
                                  .securityDefaults(
                                          SecurityHandler.create()
                                                  .queryParam(
                                                          "jwt",
                                                          TokenHandler.builder()
                                                                  .tokenHeader("BEARER_TOKEN")
                                                                  .tokenPattern(Pattern.compile("bearer (.*)"))
                                                                  .build())
                                                  .queryParam(
                                                          "name",
                                                          TokenHandler.builder()
                                                                  .tokenHeader("NAME_FROM_REQUEST")
                                                                  .build())))
                .get("/noRoles", WebSecurity.secure())
                .get("/user[/{*}]", WebSecurity.rolesAllowed("user"))
                .get("/admin", WebSecurity.rolesAllowed("admin"))
                .get("/deny", WebSecurity.rolesAllowed("deny"), (req, res) -> {
                    res.status(Http.Status.INTERNAL_SERVER_ERROR_500);
                    res.send("Should not get here, this role doesn't exist");
                })
                .get("/auditOnly", WebSecurity
                        .audit()
                        .auditEventType("unit_test")
                        .auditMessageFormat(AUDIT_MESSAGE_FORMAT)
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
}
