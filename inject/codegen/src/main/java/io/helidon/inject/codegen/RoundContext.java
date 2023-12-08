package io.helidon.inject.codegen;

import java.util.Collection;

import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;

/**
 * Context of a single round of code generation.
 * For example the first round may generate types, that require additional code generation.
 */
public interface RoundContext {
    /**
     * Available annotations for this provider.
     *
     * @return annotation types
     */
    Collection<TypeName> availableAnnotations();

    /**
     * All types for processing in this round.
     *
     * @return all type infos
     */

    Collection<TypeInfo> types();

    /**
     * All types annotated with a specific annotation.
     *
     * @param annotationType annotation type
     * @return type infos annotated with the provided annotation
     */

    Collection<TypeInfo> annotatedTypes(TypeName annotationType);

    /**
     * All elements annotated with a specific annotation.
     *
     * @param annotationType annotation type
     * @return elements annotated with the provided annotation
     */
    Collection<TypedElementInfo> annotatedElements(TypeName annotationType);
}
