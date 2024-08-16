package io.helidon.declarative.codegen.http.model;

import java.util.List;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeInfo;

@Prototype.Blueprint
interface ModelElementBlueprint {
    /**
     * All annotations on this element, and inherited from supertype/interface.
     *
     * @return annotations
     */
    @Option.Singular
    List<Annotation> annotations();

    /**
     * Type of this element.
     *
     * @return type info
     */
    TypeInfo type();
}
