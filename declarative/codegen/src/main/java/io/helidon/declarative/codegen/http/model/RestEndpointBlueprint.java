package io.helidon.declarative.codegen.http.model;

import java.util.List;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

@Prototype.Blueprint
interface RestEndpointBlueprint extends HttpAnnotatedBlueprint {
    @Option.Singular
    List<RestMethod> methods();
}
