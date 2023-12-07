package io.helidon.inject.codegen.spi;

import java.util.Optional;

import io.helidon.common.types.TypeName;
import io.helidon.inject.codegen.InjectionCodegenContext;

public interface ProviderSupport {
    /**
     * Map the type to the correct one.
     *
     * @param typeName    type of the processed injection point, may be a generic type such as {@link java.util.List},
     *                    {@link java.util.Optional} (these are the types expected to be supported)
     * @param valueSource code that obtains value from Helidon injection (if this method returns a non-empty optional,
     *                    the provided value will be an {@link java.util.Optional} {@link java.util.function.Supplier},
     *                    {@link java.util.List} of {@link java.util.function.Supplier}, or a {@link java.util.function.Supplier}
     *                    as returned by the {@link io.helidon.inject.codegen.InjectionCodegenContext.Assignment#usedType()};
     *                    other type combinations are not supported
     * @return assignment to use, or an empty assignment if this provider does not understand the type
     */
    Optional<InjectionCodegenContext.Assignment> assignment(TypeName typeName, String valueSource);
}
