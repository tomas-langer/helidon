package io.helidon.inject.codegen;

import java.util.Collection;
import java.util.Set;

import io.helidon.codegen.CodegenContext;
import io.helidon.codegen.CodegenOptions;
import io.helidon.codegen.spi.AnnotationMapper;
import io.helidon.codegen.spi.AnnotationMapperProvider;
import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeName;

@Weight(Weighted.DEFAULT_WEIGHT - 10) // lower weight than JavaxAnnotationMapper
public class MapClassNamedProvider implements AnnotationMapperProvider {
    @Override
    public Set<TypeName> supportedAnnotations() {
        return Set.of(InjectCodegenTypes.HELIDON_CLASS_NAMED);
    }

    @Override
    public AnnotationMapper create(CodegenOptions options) {
        return new ClassNamedMapper();
    }

    private static class ClassNamedMapper implements AnnotationMapper {

        private ClassNamedMapper() {
        }

        @Override
        public boolean supportsAnnotation(Annotation annotation) {
            return annotation.typeName().equals(InjectCodegenTypes.HELIDON_CLASS_NAMED);
        }

        @Override
        public Collection<Annotation> mapAnnotation(CodegenContext ctx, Annotation original, ElementKind elementKind) {
            return Set.of(Annotation.create(InjectCodegenTypes.INJECT_NAMED, original.value().orElse("")));
        }
    }
}
