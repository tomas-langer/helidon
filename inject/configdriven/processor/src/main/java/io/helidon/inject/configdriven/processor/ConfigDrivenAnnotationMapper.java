package io.helidon.inject.configdriven.processor;

import java.util.Collection;
import java.util.Set;

import io.helidon.common.processor.ProcessingContext;
import io.helidon.common.processor.spi.AnnotationMapper;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.ElementKind;
import io.helidon.inject.tools.TypeNames;

class ConfigDrivenAnnotationMapper implements AnnotationMapper {
    private static final Annotation SINGLETON = Annotation.create(TypeNames.JAKARTA_SINGLETON_TYPE);

    @Override
    public boolean supportsAnnotation(Annotation annotation) {
        return annotation.typeName().equals(ConfigDrivenAnnotation.TYPE);
    }

    @Override
    public Collection<Annotation> mapAnnotation(ProcessingContext ctx, Annotation original, ElementKind elementKind) {
        return Set.of(original, SINGLETON);
    }
}
