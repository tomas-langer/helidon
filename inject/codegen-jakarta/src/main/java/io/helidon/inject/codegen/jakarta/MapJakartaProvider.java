package io.helidon.inject.codegen.jakarta;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import io.helidon.codegen.CodegenContext;
import io.helidon.codegen.CodegenOptions;
import io.helidon.codegen.spi.AnnotationMapper;
import io.helidon.codegen.spi.AnnotationMapperProvider;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeName;
import io.helidon.inject.codegen.InjectCodegenTypes;

public class MapJakartaProvider implements AnnotationMapperProvider {
    private static final Set<TypeName> TYPES = Set.of(JakartaTypes.INJECT_SINGLETON,
                                                      JakartaTypes.INJECT_QUALIFIER,
                                                      JakartaTypes.INJECT_INJECT,
                                                      JakartaTypes.INJECT_SCOPE,
                                                      JakartaTypes.INJECT_NAMED,
                                                      JakartaTypes.INJECT_POST_CONSTRUCT,
                                                      JakartaTypes.INJECT_PRE_DESTROY);
    private static final Map<TypeName, Annotation> DIRECTLY_MAPPED = Map.of(
            JakartaTypes.INJECT_SINGLETON, Annotation.create(InjectCodegenTypes.INJECT_SINGLETON),
            JakartaTypes.INJECT_QUALIFIER, Annotation.create(InjectCodegenTypes.INJECT_QUALIFIER),
            JakartaTypes.INJECT_INJECT, Annotation.create(InjectCodegenTypes.INJECT_POINT),
            JakartaTypes.INJECT_POST_CONSTRUCT, Annotation.create(InjectCodegenTypes.INJECT_POST_CONSTRUCT),
            JakartaTypes.INJECT_PRE_DESTROY, Annotation.create(InjectCodegenTypes.INJECT_PRE_DESTROY)
    );

    @Override
    public Set<TypeName> supportedAnnotations() {
        return TYPES;
    }

    @Override
    public AnnotationMapper create(CodegenOptions options) {
        return new JakartaAnnotationMapper();
    }

    private static class JakartaAnnotationMapper implements AnnotationMapper {
        @Override
        public boolean supportsAnnotation(Annotation annotation) {
            return TYPES.contains(annotation.typeName());
        }

        @Override
        public Collection<Annotation> mapAnnotation(CodegenContext ctx, Annotation original, ElementKind elementKind) {
            TypeName typeName = original.typeName();
            Annotation annotation = DIRECTLY_MAPPED.get(typeName);
            if (annotation != null) {
                return Set.of(annotation);
            }
            // scope is mapped to nothing
            if (JakartaTypes.INJECT_SCOPE.equals(typeName)) {
                return Set.of();
            }
            // named is mapped to our named
            if (JakartaTypes.INJECT_NAMED.equals(typeName)) {
                return Set.of(Annotation.create(InjectCodegenTypes.INJECT_NAMED, original.value().orElse("")));
            }

            return Set.of(original);
        }
    }
}
