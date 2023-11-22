package io.helidon.inject.processor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.processing.RoundEnvironment;

import io.helidon.common.processor.ProcessingContext;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;

public interface RoundContext {
    static RoundContext create(ProcessingContext ctx,
                               RoundEnvironment env,
                               Collection<TypeName> annotations,
                               Map<TypeName, List<TypeInfo>> annotationsToTypes,
                               List<TypeInfo> types) {
        return new RoundContextImpl(ctx, env, annotations, annotationsToTypes, types);
    }

    Collection<TypeName> availableAnnotations();

    Collection<TypeInfo> types();

    Collection<TypeInfo> annotatedTypes(TypeName annotationType);

    Collection<TypedElementInfo> annotatedElements(TypeName annotationType);

    RoundEnvironment roundEnvironment();

    Optional<TypeInfo> createTypeInfo(TypeName typeName);
}
