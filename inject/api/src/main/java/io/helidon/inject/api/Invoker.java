package io.helidon.inject.api;

/**
 * Used from generated code.
 *
 * @param <T>
 */
@FunctionalInterface
public interface Invoker<T> {
    T invoke(Object... parameters);
}
