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

package io.helidon.security.examples.signatures;

import java.util.Set;

import io.helidon.nima.testing.junit5.webserver.ServerTest;
import io.helidon.nima.webclient.http1.Http1Client;
import io.helidon.nima.webclient.security.WebClientSecurity;
import io.helidon.nima.webserver.WebServer;
import io.helidon.security.Security;
import io.helidon.security.providers.httpauth.HttpBasicAuthProvider;

import org.junit.jupiter.api.Test;

import static io.helidon.security.providers.httpauth.HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_PASSWORD;
import static io.helidon.security.providers.httpauth.HttpBasicAuthProvider.EP_PROPERTY_OUTBOUND_USER;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
public abstract class SignatureExampleTest {

    private final Http1Client client;

    protected SignatureExampleTest(WebServer server) {
        Security security = Security.builder()
                .addProvider(HttpBasicAuthProvider.builder().build())
                .build();

        client = Http1Client.builder()
                .addService(WebClientSecurity.create(security))
                .baseUri("http://localhost:" + server.port())
                .build();
    }

    @Test
    public void testService1Hmac() {
        test("/service1", Set.of("user", "admin"), Set.of(), "Service1 - HMAC signature");
    }

    @Test
    public void testService1Rsa() {
        test("/service1-rsa", Set.of("user", "admin"), Set.of(), "Service1 - RSA signature");
    }


    private void test(String uri, Set<String> expectedRoles, Set<String> invalidRoles, String service) {
        String payload = client.get(uri)
                .property(EP_PROPERTY_OUTBOUND_USER, "jack")
                .property(EP_PROPERTY_OUTBOUND_PASSWORD, "password")
                .request(String.class);

        // check login
        assertThat(payload, containsString("id='" + "jack" + "'"));

        // check roles
        expectedRoles.forEach(role -> assertThat(payload, containsString(":" + role)));
        invalidRoles.forEach(role -> assertThat(payload, not(containsString(":" + role))));
        assertThat(payload, containsString("id='" + service + "'"));
    }
}
