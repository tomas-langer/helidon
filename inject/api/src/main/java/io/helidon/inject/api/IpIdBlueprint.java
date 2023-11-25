package io.helidon.inject.api;

import java.util.Set;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeName;

/**
 * Unique identification of an injection point.
 */
@Prototype.Blueprint
interface IpIdBlueprint {
    /**
     * Type name of the service that contains this injection point.
     *
     * @return the service declaring this injection point
     */
    TypeName service();

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
     * Descriptor declaring this dependency.
     *
     * @return descriptor
     */
    @Option.Redundant
    // kind + service type + name is a unique identification already
    TypeName descriptor();

    /**
     * Each injection point expects a specific contract to be injected.
     * For example for {@code List<MyService>}, the contract is {@code MyService}.
     *
     * @return contract of the injected service(s)
     */
    @Option.Redundant
    // kind + service type + name is a unique identification already
    TypeName contract();

    /**
     * The qualifier type annotations on this element.
     *
     * @return the qualifier type annotations on this element
     */
    @Option.Singular
    @Option.Redundant(stringValue = false)
    // kind + service type + name is a unique identification already
    Set<Qualifier> qualifiers();

    /**
     * Field name that declares this ID in the {@link #descriptor()}. Can be used for code generation.
     * This field is always a public constant.
     *
     * @return field that has the id on the descriptor
     */
    @Option.Redundant
    // kind + service type + name is a unique identification already
    String field();

    /**
     * The access modifier on the injection point/receiver.
     * Defaults to {@link io.helidon.common.types.AccessModifier#PACKAGE_PRIVATE}.
     *
     * @return the access
     */
    @Option.Default("PACKAGE_PRIVATE")
    @Option.Redundant
    // kind + service type + name is a unique identification already
    AccessModifier access();

    /**
     * True if the injection point is static.
     *
     * @return true if static receiver
     */
    @Option.Redundant // kind + service type + name is a unique identification already
    @Option.DefaultBoolean(false)
    boolean isStatic();

    /**
     * The annotations on this element.
     *
     * @return the annotations on this element
     */
    @Option.Singular
    @Option.Redundant
    // kind + service type + name is a unique identification already
    Set<Annotation> annotations();

    /**
     * Type of the injection point (exact parameter type with all generics).
     *
     * @return type of the injection point as {@link io.helidon.common.types.TypeName}
     */
    @Option.Redundant(stringValue = false)
    // kind + service type + name is a unique identification already
    TypeName typeName();

    /**
     * Create service info criteria for lookup from this injection point information.
     *
     * @return criteria to lookup matching services
     */
    default ServiceInfoCriteria toCriteria() {
        return ServiceInfoCriteria.builder()
                .qualifiers(qualifiers())
                .addContract(contract())
                .build();
    }
}
