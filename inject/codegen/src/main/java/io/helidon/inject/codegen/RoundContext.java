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
    Collection<TypeName> availableAnnotations();

    Collection<TypeInfo> types();

    Collection<TypeInfo> annotatedTypes(TypeName annotationType);

    Collection<TypedElementInfo> annotatedElements(TypeName annotationType);
}
