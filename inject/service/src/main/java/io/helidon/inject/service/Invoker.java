package io.helidon.inject.service;

/**
 * Used from generated code.
 *
 * @param <T>
 */
@FunctionalInterface
public interface Invoker<T> {
    T invoke(Object... parameters) throws Exception;
}
