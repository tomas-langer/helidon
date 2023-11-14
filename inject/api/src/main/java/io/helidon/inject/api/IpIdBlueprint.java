package io.helidon.inject.api;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeName;

/**
 * Unique identification of an injection point within a type.
 */
@Prototype.Blueprint
interface IpIdBlueprint<T> {
    /**
     * Kind of element we inject into (constructor, field, method).
     *
     * @return element kind (for parameters, the containing element)
     */
    ElementKind elementKind();

    /**
     * Unique name within a kind within a single type.
     *
     * @return unique name of the field or parameter
     */
    String name();

    /**
     * Type of the injection point (exact parameter type with all generics).
     *
     * @return type of the injection point as {@link io.helidon.common.types.TypeName}
     */
    @Option.Redundant
    TypeName typeName();
}
