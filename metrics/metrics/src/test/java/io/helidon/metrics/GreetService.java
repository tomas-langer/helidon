/*
 * Copyright (c) 2021, 2022 Oracle and/or its affiliates.
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
package io.helidon.metrics;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import io.helidon.common.configurable.ThreadPoolSupplier;
import io.helidon.config.Config;
import io.helidon.reactive.webserver.Routing;
import io.helidon.reactive.webserver.ServerRequest;
import io.helidon.reactive.webserver.ServerResponse;
import io.helidon.reactive.webserver.Service;

class GreetService implements Service {

    static final int SLOW_DELAY_SECS = 1;

    static final String GREETING_RESPONSE = "Hello World!";

    static CountDownLatch slowRequestInProgress = null;

    static void initSlowRequest() {
        slowRequestInProgress = new CountDownLatch(1);
    }

    static void awaitSlowRequestStarted() throws InterruptedException {
        slowRequestInProgress.await();
    }

    private final ExecutorService executorService;

   GreetService() {
       Config config = Config.create();
       executorService = ThreadPoolSupplier.builder()
               .config(config.get("application-thread-pool"))
               .build()
               .get();
   }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/greet/slow", this::greetSlow);
    }

    private void greetSlow(ServerRequest request, ServerResponse response) {
        executorService.submit(() -> {
            if (slowRequestInProgress != null) {
                slowRequestInProgress.countDown();
            }
            try {
                TimeUnit.SECONDS.sleep(SLOW_DELAY_SECS);
            } catch (InterruptedException e) {
                //absorb silently
            }
            response.send(GREETING_RESPONSE);
        });
    }
}
