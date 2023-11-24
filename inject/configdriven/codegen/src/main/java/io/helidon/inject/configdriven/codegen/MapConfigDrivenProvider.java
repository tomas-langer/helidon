package io.helidon.inject.configdriven.codegen;

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
import io.helidon.inject.codegen.InjectCodegenTypes;

@Weight(Weighted.DEFAULT_WEIGHT - 10)
public class MapConfigDrivenProvider implements AnnotationMapperProvider {
    private static final Annotation SINGLETON = Annotation.create(InjectCodegenTypes.INJECT_SINGLETON);

    @Override
    public Set<TypeName> supportedAnnotations() {
        return Set.of(ConfigDrivenAnnotation.TYPE);
    }

    @Override
    public AnnotationMapper create(CodegenOptions options) {
        return new ConfigDrivenMapper();
    }

    private static class ConfigDrivenMapper implements AnnotationMapper {
        @Override
        public boolean supportsAnnotation(Annotation annotation) {
            return annotation.typeName().equals(ConfigDrivenAnnotation.TYPE);
        }

        @Override
        public Collection<Annotation> mapAnnotation(CodegenContext ctx, Annotation original, ElementKind elementKind) {
            return Set.of(original, SINGLETON);
        }
    }
}
