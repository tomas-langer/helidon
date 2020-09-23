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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.testsupport.TestClient;
import io.helidon.webserver.testsupport.TestResponse;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@HelidonReactiveTest(local = true)
class LocalTest {
    @AddRoute("/greet")
    static void greetRoute(ServerRequest req, ServerResponse res) {
        String name = req.queryParams().first("name").orElse("World");
        res.send("Hello " + name);
    }

    @Test
    void testRoute(TestClient client) throws TimeoutException, InterruptedException, ExecutionException {
        TestResponse testResponse = client.path("/greet")
                .queryParameter("name", "Helidon")
                .get();

        String response = testResponse.asString()
                .get(10, TimeUnit.SECONDS);

        assertThat(response, is("Hello Helidon"));
    }
}
