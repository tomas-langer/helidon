package io.helidon.inject.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.processing.RoundEnvironment;

import io.helidon.common.processor.ProcessingContext;
import io.helidon.common.processor.TypeInfoFactory;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;

class RoundContextImpl implements RoundContext {
    // within one round, we can safely cache
    private final Map<TypeName, Optional<TypeInfo>> typeInfoCache = new ConcurrentHashMap<>();
    private final ProcessingContext ctx;
    private final RoundEnvironment env;
    private final Map<TypeName, List<TypeInfo>> annotationToTypes;
    private final List<TypeInfo> types;
    private final Collection<TypeName> annotations;

    RoundContextImpl(ProcessingContext ctx,
                     RoundEnvironment env,
                     Collection<TypeName> annotations,
                     Map<TypeName, List<TypeInfo>> annotationToTypes,
                     List<TypeInfo> types) {
        this.ctx = ctx;
        this.env = env;
        this.annotations = annotations;
        this.annotationToTypes = annotationToTypes;
        this.types = types;
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
        List<TypeInfo> typeInfos = annotationToTypes.get(annotationType);
        if (typeInfos == null) {
            return Set.of();
        }

        List<TypedElementInfo> result = new ArrayList<>();

        for (TypeInfo typeInfo : typeInfos) {
            typeInfo.elementInfo()
                    .stream()
                    .filter(it -> it.hasAnnotation(annotationType))
                    .forEach(result::add);
        }

        return result;
    }

    @Override
    public Collection<TypeInfo> annotatedTypes(TypeName annotationType) {
        List<TypeInfo> typeInfos = annotationToTypes.get(annotationType);
        if (typeInfos == null) {
            return Set.of();
        }

        List<TypeInfo> result = new ArrayList<>();

        for (TypeInfo typeInfo : typeInfos) {
            if (typeInfo.hasAnnotation(annotationType)) {
                result.add(typeInfo);
            }
        }

        return result;
    }

    @Override
    public RoundEnvironment roundEnvironment() {
        return env;
    }

    @Override
    public Optional<TypeInfo> createTypeInfo(TypeName typeName) {
        return typeInfoCache.computeIfAbsent(typeName, it -> TypeInfoFactory.create(ctx, typeName));
    }
}
