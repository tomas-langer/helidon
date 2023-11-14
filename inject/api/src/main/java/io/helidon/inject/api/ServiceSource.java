package io.helidon.inject.api;

import java.util.ArrayList;
import java.util.List;

import io.helidon.common.types.ElementKind;

/**
 * Usually code generated source of a service. In addition to providing service metadata, this also allows instantiation
 * and injection to the service instance. Methods from this interface are expected to be code generated (if applicable).
 *
 * @param <T> type of the service implementation
 */
public interface ServiceSource<T> extends ServiceDescriptor<T> {
    /**
     * Create a new service instance.
     *
     * @param ctx injection context with all injection points data
     * @return a new instance, must be of the type T or a subclass
     */
    // we cannot return T, as it does not allow us to correctly handle inheritance
    default Object instantiate(InjectionContext ctx, InterceptionMetadata interceptionMetadata) {
        throw new IllegalStateException("Cannot instantiate type " + serviceType().fqName() + ", as it is either abstract,"
                                                + " or an interface.");
    }

    /**
     * Inject methods.
     *
     * @param ctx      injection context
     * @param instance instance to update
     */

    default void injectMethods(InjectionContext ctx, T instance) {
    }

    /**
     * Inject fields.
     *
     * @param ctx      injection context
     * @param instance instance to update
     */

    default void injectFields(InjectionContext ctx, InterceptionMetadata interceptionMetadata, T instance) {
    }

    /**
     * Invoke {@link jakarta.annotation.PostConstruct} annotated method(s).
     */
    default void postConstruct(T instance) {
    }

    /**
     * Invoke {@link jakarta.annotation.PreDestroy} annotated method(s).
     */
    default void preDestroy(T instance) {
    }

    /**
     * Combine dependencies from this type with dependencies from supertype.
     * This is a utility for code generated types.
     *
     * @param myType this type's dependencies
     * @param superType super type's dependencies
     * @return a new list without constructor dependencies from super type
     */
    default List<ServiceDependencies> combineDependencies(ServiceDependencies myType, List<ServiceDependencies> superType) {
        List<ServiceDependencies> result = new ArrayList<>();
        result.add(myType);

        for (ServiceDependencies superDeps : superType) {
            List<IpInfo> list = superDeps.dependencies()
                    .stream()
                    .filter(it -> it.id().elementKind() != ElementKind.CONSTRUCTOR)
                    .toList();

            if (!list.isEmpty()) {
                result.add(new ServiceDependencies(superDeps.serviceType(), List.copyOf(list)));
            }
        }

        return List.copyOf(result);
    }
}
