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
public class MapApplicationScopedProvider implements AnnotationMapperProvider {
    private static final Annotation SINGLETON = Annotation.create(InjectCodegenTypes.INJECT_SINGLETON);

    @Override
    public Set<String> supportedOptions() {
        return Set.of(InjectOptions.MAP_APPLICATION_TO_SINGLETON_SCOPE);
    }

    @Override
    public Set<TypeName> supportedAnnotations() {
        return Set.of(CdiTypes.APPLICATION_SCOPED);
    }

    @Override
    public AnnotationMapper create(CodegenOptions options) {
        return new ApplicationScopedMapper(options.option(InjectOptions.MAP_APPLICATION_TO_SINGLETON_SCOPE, false));
    }

    private static class ApplicationScopedMapper implements AnnotationMapper {
        private final boolean enabled;

        private ApplicationScopedMapper(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public boolean supportsAnnotation(Annotation annotation) {
            return enabled && annotation.typeName().equals(CdiTypes.APPLICATION_SCOPED);
        }

        @Override
        public Collection<Annotation> mapAnnotation(CodegenContext ctx, Annotation original, ElementKind elementKind) {
            return Set.of(original, SINGLETON);
        }
    }
}
