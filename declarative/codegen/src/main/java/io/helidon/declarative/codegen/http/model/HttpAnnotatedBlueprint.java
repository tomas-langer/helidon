package io.helidon.declarative.codegen.http.model;

import java.util.List;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

@Prototype.Blueprint(createEmptyPublic = false)
interface HttpAnnotatedBlueprint extends ModelElementBlueprint {

    @Option.Singular
    List<String> produces();

    @Option.Singular
    List<String> consumes();

    @Option.Singular
    List<HeaderValue> headers();

    @Option.Singular
    List<ComputedHeader> computedHeaders();

    Optional<String> path();
}
