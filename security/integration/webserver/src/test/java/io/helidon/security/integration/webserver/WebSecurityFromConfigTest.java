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

import io.helidon.common.http.MediaType;
import io.helidon.config.Config;
import io.helidon.security.Security;
import io.helidon.security.SecurityContext;
import io.helidon.webclient.WebClient;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.junit5.AddRoute;
import io.helidon.webserver.junit5.AddService;

/**
 * Unit test for {@link WebSecurity}.
 */
public class WebSecurityFromConfigTest extends WebSecurityTests {
    WebSecurityFromConfigTest(WebServer server, WebClient webClient) {
        super(server, webClient);
    }

    @AddService(WebSecurity.class)
    static WebSecurity setupSecurity(Config config) {
        WebSecurityTestUtil.auditLogFinest();

        myAuditProvider = new UnitTestAuditProvider();

        Config securityConfig = config.get("security");

        Security security = Security.builder(securityConfig)
                .addAuditProvider(myAuditProvider).build();
        return WebSecurity.create(security, securityConfig);
    }

    @AddRoute("/{*}")
    static void routing(ServerRequest req, ServerResponse res) {
        Optional<SecurityContext> securityContext = req.context().get(SecurityContext.class);
        res.headers().contentType(MediaType.TEXT_PLAIN.withCharset("UTF-8"));
        res.send("Hello, you are: \n" + securityContext
                .map(ctx -> ctx.user().orElse(SecurityContext.ANONYMOUS).toString())
                .orElse("Security context is null"));
    }
}
