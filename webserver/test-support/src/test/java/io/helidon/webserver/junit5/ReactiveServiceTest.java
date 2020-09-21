/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

package io.helidon.webserver.junit5;

import java.util.concurrent.TimeUnit;

import io.helidon.config.Config;
import io.helidon.webclient.WebClient;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@HelidonReactiveTest
@AddService(ReactiveServiceTest.ReactiveService.class)
@AddConfig(key = "service", value = "value")
class ReactiveServiceTest {
    private static boolean beforeAllCalled;
    private boolean beforeEachCalled;

    private final WebClient webClient;

    ReactiveServiceTest(WebClient webClient) {
        this.webClient = webClient;
    }

    @AddRoute("/greet")
    static void route1(ServerRequest req, ServerResponse res) {
        res.send("Hello World");
    }

    @AddService(value = ReactiveService.class, path = "/method")
    static ReactiveService service1(Config config) {
        assertThat(config, notNullValue());
        assertThat(config.get("service").asString().get(), is("value"));
        return new ReactiveService("Method");
    }

    @BeforeAll
    static void initClass() {
        beforeAllCalled = true;
    }

    @BeforeEach
    void beforeEach() {
        beforeEachCalled = true;
    }

    @Test
    void testRoute() {
        String result = webClient.get()
                .path("/greet")
                .request(String.class)
                .await(10, TimeUnit.SECONDS);

        assertThat(result, is("Hello World"));
    }

    @Test
    void testServiceRoute() {
        String result = webClient.get()
                .path("/service")
                .request(String.class)
                .await(10, TimeUnit.SECONDS);

        assertThat(result, is("Hello From Service"));
    }

    @Test
    void testServiceFromMethod() {
        String result = webClient.get()
                .path("/method/service")
                .request(String.class)
                .await(10, TimeUnit.SECONDS);

        assertThat(result, is("Hello From Method"));
    }

    @Test
    void testLifecycleMethodsCalled() {
        // this is to validate we can still use the usual junit methods
        assertThat("Before all should have been called", beforeAllCalled, is(true));
        assertThat("Before each should have been called", beforeEachCalled, is(true));
    }

    static class ReactiveService implements Service {
        private final String where;

        ReactiveService() {
            this("Service");
        }

        ReactiveService(String where) {
            this.where = where;
        }

        @Override
        public void update(Routing.Rules rules) {
            rules.get("/service", this::greet);
        }

        private void greet(ServerRequest req, ServerResponse res) {
            res.send("Hello From " + where);
        }
    }
}