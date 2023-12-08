package io.helidon.inject.service;

/**
 * Invocation of an element that has parameters, and may throw checked exceptions.
 *
 * @param <T> type of the result of the invocation
 */
@FunctionalInterface
public interface Invoker<T> {
    /**
     * Invoke the element.
     *
     * @param parameters to pass to the element
     * @return result of the invocation
     * @throws Exception any exception that may be required by the invoked element
     */
    T invoke(Object... parameters) throws Exception;
}
