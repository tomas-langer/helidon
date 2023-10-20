package io.helidon.inject.processor;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;

class RoundContextImpl implements RoundContext {
    private final RoundEnvironment env;
    private final Collection<TypeInfo> types;
    private final Collection<TypeName> annotations;
    private final Map<TypeName, TypeElement> typesToElements;

    RoundContextImpl(RoundEnvironment env,
                     Collection<TypeInfo> types,
                     Collection<TypeName> annotations,
                     Map<TypeName, TypeElement> typesToElements) {
        this.env = env;
        this.types = types;
        this.annotations = annotations;
        this.typesToElements = typesToElements;
    }

    @Override
    public Collection<TypeName> availableAnnotations() {
        return annotations;
    }

    @Override
    public Collection<TypeInfo> types() {
        return types;
    }

    @Override
    public Collection<TypedElementInfo> annotatedElements(TypeName annotationType) {
        return Set.of();
    }

    @Override
    public Collection<TypeInfo> annotatedTypes(TypeName annotationType) {
        return Set.of();
    }

    @Override
    public RoundEnvironment roundEnvironment() {
        return env;
    }
}
