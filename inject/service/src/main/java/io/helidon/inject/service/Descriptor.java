package io.helidon.inject.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeName;

/**
 * A descriptor of a service. In addition to providing service metadata, this also allows instantiation
 * and injection to the service instance.
 * <p>
 * Methods from this interface are expected to be code generated (if applicable).
 *
 * @param <T> type of the service implementation
 */
public interface Descriptor<T> extends ServiceInfo<T> {
    /**
     * Create a new service instance.
     *
     * @param ctx                  injection context with all injection points data
     * @param interceptionMetadata interception metadata to use when the constructor should be intercepted
     * @return a new instance, must be of the type T or a subclass
     */
    // we cannot return T, as it does not allow us to correctly handle inheritance
    default Object instantiate(InjectionContext ctx, InterceptionMetadata interceptionMetadata) {
        throw new IllegalStateException("Cannot instantiate type " + serviceType().fqName() + ", as it is either abstract,"
                                                + " or an interface.");
    }

    /**
     * Inject fields and methods.
     *
     * @param ctx                  injection context
     * @param interceptionMetadata interception metadata to support interception of field injection
     * @param injected             mutable set of already injected methods from subtypes
     * @param instance             instance to update
     */

    default void inject(InjectionContext ctx,
                        InterceptionMetadata interceptionMetadata,
                        Set<MethodSignature> injected,
                        T instance) {
    }

    /**
     * Invoke {@link io.helidon.inject.service.Inject.PostConstruct} annotated method(s).
     *
     * @param instance instance to use
     */
    default void postConstruct(T instance) {
    }

    /**
     * Invoke {@link io.helidon.inject.service.Inject.PreDestroy} annotated method(s).
     *
     * @param instance instance to use
     */
    default void preDestroy(T instance) {
    }

    /**
     * Combine dependencies from this type with dependencies from supertype.
     * This is a utility for code generated types.
     *
     * @param myType    this type's dependencies
     * @param superType super type's dependencies
     * @return a new list without constructor dependencies from super type
     */
    default List<IpId> combineDependencies(List<IpId> myType, List<IpId> superType) {
        List<IpId> result = new ArrayList<>(myType);

        result.addAll(superType.stream()
                              .filter(it -> it.elementKind() != ElementKind.CONSTRUCTOR)
                              .toList());

        return List.copyOf(result);
    }

    /**
     * Method signature uniquely identifies a method by its signature.
     * The declaring class is the top level class that declares the method. This allows us to identify overridden methods.
     *
     * @param declaringType  top level type that declares the method, may be inaccessible (so cannot use class)
     * @param name           name of the method
     * @param parameterTypes string representation of fully qualified (and with generic declaration) parameter types
     */
    record MethodSignature(TypeName declaringType, String name, List<String> parameterTypes) {
        /**
         * A simplified constructor for methods with no parameters.
         *
         * @param declaringType top level type that declares the method, may be inaccessible (so cannot use class)
         * @param name name of the method
         */
        public MethodSignature(TypeName declaringType, String name) {
            this(declaringType, name, List.of());
        }
    }
}
