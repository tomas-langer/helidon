package io.helidon.inject.codegen.spi;

import java.util.Optional;

import io.helidon.common.types.TypeName;
import io.helidon.inject.codegen.InjectionCodegenContext;

/**
 * Provides customized assignments for injected types.
 * <p>
 * When supporting third party injection frameworks (such as Jakarta Inject - JSR-330), it is quite easy to map annotations
 * to Helidon equivalents, but we also need to support some prescribed types for injection.
 * For example in Jakarta we need to support {@code Provider} type (same as {@link java.util.function.Supplier}, just predates
 * its existence).
 * As we need to assign the correct type to injection points, and it must behave similar to our Service provider, we need
 * to provide source code mapping from {@link java.util.function.Supplier} to the type (always three: plain type,
 * {@link java.util.Optional} type, and a {@link java.util.List} of types).
 */
public interface InjectAssignment {
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
