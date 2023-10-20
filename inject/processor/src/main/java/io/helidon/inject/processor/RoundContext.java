package io.helidon.inject.processor;

import java.util.Collection;
import java.util.Map;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;

public interface RoundContext {
    static RoundContext create(RoundEnvironment env,
                               Collection<TypeName> annotations,
                               Map<TypeName, TypeElement> typesToElements,
                               Collection<TypeInfo> types) {
        return new RoundContextImpl(env, types, annotations, typesToElements);
    }

    Collection<TypeName> availableAnnotations();

    Collection<TypeInfo> types();

    Collection<TypeInfo> annotatedTypes(TypeName annotationType);

    Collection<TypedElementInfo> annotatedElements(TypeName annotationType);

    RoundEnvironment roundEnvironment();
}
