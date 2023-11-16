package io.helidon.inject.api;

import io.helidon.builder.api.Prototype;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeName;

/**
 * Unique identification of an injection point.
 *
 * @param <T> type of the injection point, to support easy assignment in service sources
 */
@Prototype.Blueprint
interface IpIdBlueprint<T> {
    /**
     * Type name of the service that contains this injection point.
     *
     * @return the service declaring this injection point
     */
    TypeName serviceType();
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
}
