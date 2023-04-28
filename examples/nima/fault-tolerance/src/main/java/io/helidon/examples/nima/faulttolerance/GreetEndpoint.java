/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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

package io.helidon.examples.nima.faulttolerance;

import java.time.temporal.ChronoUnit;

import io.helidon.common.http.Entity;
import io.helidon.common.http.GET;
import io.helidon.common.http.HeaderParam;
import io.helidon.common.http.Http;
import io.helidon.common.http.POST;
import io.helidon.common.http.Path;
import io.helidon.common.http.PathParam;
import io.helidon.common.http.QueryParam;
import io.helidon.nima.faulttolerance.FaultTolerance;
import io.helidon.pico.api.PicoException;
import io.helidon.pico.api.PicoServices;
import io.helidon.pico.api.Services;

import jakarta.inject.Singleton;

@Singleton
@Path("/greet")
class GreetEndpoint {
    private final Services services;
    private String greeting = "Hello";

    GreetEndpoint() {
        this.services = PicoServices.realizedServices();
    }

    public static void main(String[] args) {
        /*
        Nima.start();

        WebServer.create(WebServerConfig.builder()
                                 .port(8080)
                                 .host("localhost")
                                 .config(config)
                                 .build());
        WebServer.create(builder -> builder.host("localhost")
                        .port(8080)
                        .config(config.get("server"))
                        .routing(GreetEndpoint::routing))
                .start();

        WebServer.builder()
                .socket("https", builder -> {

                })
                .routing(routingBuilder -> {

                })
                .start();

        Timed timeout = Timed.create(TimedSomething.builder()....);

                Retry retry = services.lookup(Retry.class).get();

        // yes
        Retry manualRetryb = Retry.create(builder -> builder.calls(5)
                .skipOn(Set.of(Throwable.class)));

        // TODO change the suffix/prefix defaults
        // yes
        Retry manualRetry = Retry.create(RetryConfigDefaults.builder()
                                                 .config(theConfig)
                                                 .calls(5));

         */
    }

    static String greetNamedFallback(String name,
                                     String shouldThrow,
                                     String hostHeader,
                                     Throwable t) {
        return "Fallback for \"greetNamed\": Failed to obtain greeting for " + name + ", message: " + t.getMessage();
    }

    @GET
    String greet() {
        return greeting + " World!";
    }

    //    @Http.GET
    //    @Http.Path("/{name}")
    //    @Metrics.Timed()
    @GET
    @Path("/{name}")
    @FaultTolerance.Retry(name = "someName", calls = 2, delayTime = 1, timeUnit = ChronoUnit.SECONDS, overallTimeout = 10,
                          applyOn = PicoException.class, skipOn = {OutOfMemoryError.class, StackOverflowError.class})
    @FaultTolerance.Fallback("greetNamedFallback")
    @FaultTolerance.CircuitBreaker
    @FaultTolerance.Bulkhead(name = "bulkhead-it")
    @FaultTolerance.Async(name = "myname")
    // should not be used on web methods, as http is not thread safe, just a demo, will be removed from example
    String greetNamed(@PathParam("name") String name,
                      @QueryParam(value = "throw", defaultValue = "false") String shouldThrow,
                      @HeaderParam(Http.Header.HOST_STRING) String hostHeader) {
        System.out.println(Thread.currentThread().getName());
        if ("true".equalsIgnoreCase(shouldThrow)) {
            throw new PicoException("Failed on purpose");
        }
        return greeting + " " + name + "! Requested host: " + hostHeader;
    }

    @POST
    void post(@Entity String message) {
        greeting = message;
    }
}
