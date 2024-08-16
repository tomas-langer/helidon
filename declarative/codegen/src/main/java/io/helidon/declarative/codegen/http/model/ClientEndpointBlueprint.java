package io.helidon.declarative.codegen.http.model;

import java.util.Optional;

import io.helidon.builder.api.Prototype;

@Prototype.Blueprint
interface ClientEndpointBlueprint extends RestEndpointBlueprint {

    Optional<String> uri();

    String configKey();

    Optional<String> clientName();
}
