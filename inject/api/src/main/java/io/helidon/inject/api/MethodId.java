package io.helidon.inject.api;

import io.helidon.common.GenericType;

/**
 * Unique identification of a method.
 *
 * @param name      name of the method
 * @param arguments arguments of the method
 */
public record MethodId(String name, GenericType<?>... arguments) {
}
