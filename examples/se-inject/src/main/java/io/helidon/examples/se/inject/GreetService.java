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

package io.helidon.examples.se.inject;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.helidon.Entity;
import io.helidon.Status;
import io.helidon.common.http.Http;
import io.helidon.common.reactive.Single;
import io.helidon.dbclient.DbClient;
import io.helidon.microprofile.server.RoutingName;
import io.helidon.microprofile.server.RoutingPath;
import io.helidon.webclient.WebClient;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@RoutingPath("/greet")
@RoutingName("port")
public class GreetService {
    @Inject
    @Named("service-1")
    private WebClient webClient;

    @Inject
    private GreetClient greetClient;

    @Inject
    @Named("pokemon-db")
    private DbClient dbClient;

    @Inject
    @ConfigProperty
    @Named("app.greeting")
    private String greeting;

    @GET
    @Path("/")
    public Single<String> defaultGreeting() {
        return Single.just(greeting + " World");
    }

    @GET
    @Path("/{name}")
    public Single<String> greeting(@PathParam("name") String name) {
        return Single.just(greeting + " " + name);
    }

    @PUT
    @Path("/greeting")
    @Status(Http.Status.CREATED_201)
    public Single<String> updateGreeting(@Entity String newGreeting) {
        this.greeting = newGreeting;
        return Single.just("Updated greeting to " +newGreeting);
    }
}
