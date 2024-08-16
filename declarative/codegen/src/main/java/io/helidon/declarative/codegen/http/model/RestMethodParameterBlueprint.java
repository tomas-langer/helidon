package io.helidon.declarative.codegen.http.model;

import io.helidon.builder.api.Prototype;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;

@Prototype.Blueprint
interface RestMethodParameterBlueprint extends ModelElementBlueprint {
    String name();

    TypeName typeName();

    int index();

    TypedElementInfo method();

    TypedElementInfo parameter();
}
