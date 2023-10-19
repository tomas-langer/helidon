package io.helidon.common.processor.spi;

import io.helidon.common.processor.ProcessingContext;
import io.helidon.common.types.TypeInfo;

/**
 * Maps {@link io.helidon.common.types.TypeInfo} to another {@link io.helidon.common.types.TypeInfo}.
 * This mapper can be used to handle complex changes to a definition of a type, such as combining
 * multiple annotations into a single one.
 */
public interface TypeMapper {
    boolean supportsType(TypeInfo type);
    TypeInfo map(ProcessingContext ctx, TypeInfo original);
}
