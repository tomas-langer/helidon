package io.helidon.inject.api;

import java.util.Optional;

import io.helidon.builder.api.Prototype;
import io.helidon.common.GenericType;
import io.helidon.common.types.ElementKind;

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
     * Name of the field or parameter.
     *
     * @return name of the field or parameter
     */
    String name();

    /**
     * Type of the injection point (exact parameter type).
     *
     * @return type of the injection point
     */
    GenericType<T> type();

    /**
     * Unique method identification within a type (name and parameter types).
     *
     * @return method id
     */
    Optional<MethodId> method();
}
