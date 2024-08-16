package io.helidon.declarative.codegen.http.model;

import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

@Prototype.Blueprint
interface ServerEndpointBlueprint extends RestEndpointBlueprint {
    Optional<String> listener();

    @Option.DefaultBoolean(false)
    boolean listenerRequired();
}
