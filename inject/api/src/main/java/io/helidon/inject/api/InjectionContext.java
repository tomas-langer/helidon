package io.helidon.inject.api;

import io.helidon.common.GenericType;
import io.helidon.common.types.ElementKind;

/**
 * All data needed for creating an instance
 * of a service, or for invoking methods that use
 * context. The context contains only the services needed
 * for the specific location.
 */
public interface InjectionContext {
    /**
     * Obtain a parameter for a specific id.
     * The ID must be known in advance and provided through {@link io.helidon.inject.api.ServiceDescriptor}.
     *
     * @param paramId parameter ID
     * @return value for the parameter, this may be null
     * @param <T> type of the parameter
     */
    <T> T param(InjectionParameterId<T> paramId);

    /**
     * Id of an injection parameter.
     *
     * @param kind kind of injection point (field, constructor, method)
     * @param elementName name of the element we inject into
     * @param paramName name of the parameter (if multiple, otherwise same as element name)
     * @param type type of the parameter
     * @param methodId to uniquely identify a method (name + arguments guarantee a unique signature)
     * @param <T> type of the parameter
     */
    record InjectionParameterId<T>(ElementKind kind,
                                   String elementName,
                                   String paramName,
                                   GenericType<T> type,
                                   MethodId methodId) {
        /**
         * Constructor to use for non-method injection parameters (constructor, field).
         *
         * @param kind kind of injection point (field, constructor, method)
         * @param elementName name of the element we inject into
         * @param paramName name of the parameter (if multiple, otherwise same as element name)
         * @param type type of the parameter
         */
        public InjectionParameterId(ElementKind kind,
                                    String elementName,
                                    String paramName,
                                    GenericType<T> type) {
            this(kind, elementName, paramName, type, null);
        }
    }

    /**
     * Unique identification of a method.
     *
     * @param name name of the method
     * @param arguments arguments of the method
     */
    record MethodId(String name, GenericType<?>... arguments) {
    }
}
