package io.helidon.inject.api;

import java.util.Set;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeName;

/**
 * Injection point information.
 * Each injection point has two set of characteristics:
 * <nl>
 * <li>Unique identification of the point we are injecting into (field, method parameter)</li>
 * <li>Information required to obtain correct service instance(s) to inject (qualifiers, annotations)</li>
 * </nl>
 */
@Prototype.Blueprint
interface IpInfoBlueprint {
    /**
     * Identification of this injection point (unique id of the field, method parameter, constructor parameter).
     *
     * @return identification
     */
    @Option.Type("IpId<?>")
    IpId<?> id();

    /**
     * Field name that declares the {@link #id()}, so code generation can directly reference it.
     *
     * @return field that has the id on the descriptor
     */
    String field();

    /**
     * The access modifier on the injection point/receiver.
     * Defaults to {@link io.helidon.common.types.AccessModifier#PACKAGE_PRIVATE}.
     *
     * @return the access
     */
    @Option.Default("PACKAGE_PRIVATE")
    AccessModifier access();

    /**
     * True if the injection point is static.
     *
     * @return true if static receiver
     */
    @Option.DefaultBoolean(false)
    boolean isStatic();

    /**
     * The annotations on this element.
     *
     * @return the annotations on this element
     */
    @Option.Singular
    Set<Annotation> annotations();

    /**
     * The qualifier type annotations on this element.
     *
     * @return the qualifier type annotations on this element
     */
    @Option.Singular
    Set<Qualifier> qualifiers();

    /**
     * Each injection point expects a specific contract to be injected.
     * For example for {@code List<MyService>}, the contract is {@code MyService}.
     *
     * @return contract of the injected service(s)
     */
    TypeName contract();

    /**
     * Type of the injection point (exact parameter type with all generics).
     *
     * @return type of the injection point as {@link io.helidon.common.types.TypeName}
     */
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
