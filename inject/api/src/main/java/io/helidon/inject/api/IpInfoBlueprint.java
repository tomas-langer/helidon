package io.helidon.inject.api;

import java.util.Set;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotation;

/**
 * Injection point information.
 * Each injection point has two set of characteristics:
 * <nl>
 *     <li>Unique identification of the point we are injecting into (field, method parameter)</li>
 *     <li>Information required to obtain correct service instance(s) to inject (qualifiers, annotations)</li>
 * </nl>
 */
@Prototype.Blueprint
interface IpInfoBlueprint<T> {
    /**
     * Identification of this injection point (unique id of the field, method parameter, constructor parameter).
     *
     * @return identification
     */
    @Option.Type("IpId<T>")
    IpId<T> id();

    /**
     * The access modifier on the injection point/receiver.
     *
     * @return the access
     */
    AccessModifier access();

    /**
     * True if the injection point is static.
     *
     * @return true if static receiver
     */
    @Option.DefaultBoolean(false)
    boolean isStatic();

    /**
     * The enclosing class name for the element.
     *
     * @return service type name
     */
    Class<?> serviceType();

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
}
