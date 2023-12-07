package io.helidon.inject.codegen.javax;

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

public class MapJavaxProvider implements AnnotationMapperProvider {
    private static final Set<TypeName> TYPES = Set.of(JavaxTypes.INJECT_SINGLETON,
                                                      JavaxTypes.INJECT_QUALIFIER,
                                                      JavaxTypes.INJECT_INJECT,
                                                      JavaxTypes.INJECT_SCOPE,
                                                      JavaxTypes.INJECT_NAMED,
                                                      JavaxTypes.INJECT_POST_CONSTRUCT,
                                                      JavaxTypes.INJECT_PRE_DESTROY);

    private static final Map<TypeName, Annotation> DIRECTLY_MAPPED = Map.of(
            JavaxTypes.INJECT_SINGLETON, Annotation.create(InjectCodegenTypes.INJECT_SINGLETON),
            JavaxTypes.INJECT_QUALIFIER, Annotation.create(InjectCodegenTypes.INJECT_QUALIFIER),
            JavaxTypes.INJECT_INJECT, Annotation.create(InjectCodegenTypes.INJECT_POINT),
            JavaxTypes.INJECT_POST_CONSTRUCT, Annotation.create(InjectCodegenTypes.INJECT_POST_CONSTRUCT),
            JavaxTypes.INJECT_PRE_DESTROY, Annotation.create(InjectCodegenTypes.INJECT_PRE_DESTROY)
    );

    @Override
    public Set<TypeName> supportedAnnotations() {
        return TYPES;
    }

    @Override
    public AnnotationMapper create(CodegenOptions options) {
        return new JavaxAnnotationMapper();
    }

    private static class JavaxAnnotationMapper implements AnnotationMapper {
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
            if (JavaxTypes.INJECT_SCOPE.equals(typeName)) {
                return Set.of();
            }
            // named is mapped to our named
            if (JavaxTypes.INJECT_NAMED.equals(typeName)) {
                return Set.of(Annotation.create(InjectCodegenTypes.INJECT_NAMED, original.value().orElse("")));
            }

            return Set.of(original);
        }
    }
}
