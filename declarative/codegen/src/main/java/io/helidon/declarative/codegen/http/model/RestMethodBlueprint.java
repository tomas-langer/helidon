package io.helidon.declarative.codegen.http.model;

import java.util.List;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;

@Prototype.Blueprint
interface RestMethodBlueprint extends HttpAnnotatedBlueprint {
    Optional<HttpStatus> status();

    TypeName returnType();

    String name();
    String uniqueName();

    HttpMethod httpMethod();

    @Option.Singular
    List<RestMethodParameter> parameters();

    Optional<RestMethodParameter> entityParameter();

    @Option.Singular
    List<RestMethodParameter> headerParameters();

    @Option.Singular
    List<RestMethodParameter> queryParameters();

    @Option.Singular
    List<RestMethodParameter> pathParameters();

    TypedElementInfo method();

    TypeInfo type();
}
