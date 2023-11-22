package io.helidon.inject.processor;

import java.util.Collection;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.common.processor.AptOptions;
import io.helidon.common.processor.ProcessingContext;
import io.helidon.common.processor.spi.AnnotationMapper;
import io.helidon.common.processor.spi.AnnotationMapperProvider;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeName;
import io.helidon.inject.tools.Options;
import io.helidon.inject.tools.TypeNames;

@Weight(Weighted.DEFAULT_WEIGHT - 10) // lower weight than JavaxAnnotationMapper
public class ApplicationScopedMapperProvider implements AnnotationMapperProvider {
    private static final TypeName APPLICATION_SCOPED = TypeName.create("jakarta.enterprise.context.ApplicationScoped");
    private static final Annotation SINGLETON = Annotation.create(TypeNames.JAKARTA_SINGLETON_TYPE);

    @Override
    public Collection<String> supportedOptions() {
        return Set.of(Options.TAG_MAP_APPLICATION_TO_SINGLETON_SCOPE);
    }

    @Override
    public Collection<TypeName> supportedTypes() {
        return Set.of(APPLICATION_SCOPED);
    }

    @Override
    public AnnotationMapper create(ProcessingEnvironment aptEnv, AptOptions options) {
        return new ApplicationScopedMapper(options.option(Options.TAG_MAP_APPLICATION_TO_SINGLETON_SCOPE, false));
    }

    private static class ApplicationScopedMapper implements AnnotationMapper {
        private final boolean enabled;

        private ApplicationScopedMapper(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public boolean supportsAnnotation(Annotation annotation) {
            return enabled && annotation.typeName().equals(APPLICATION_SCOPED);
        }

        @Override
        public Collection<Annotation> mapAnnotation(ProcessingContext ctx, Annotation original, ElementKind elementKind) {
            return Set.of(original, SINGLETON);
        }
    }
}
