package io.helidon.di.processor;

import java.lang.annotation.Annotation;
import java.util.List;

import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.annotation.NamedAnnotationMapper;
import io.micronaut.inject.visitor.VisitorContext;

public class MetricsSimplyTimed implements NamedAnnotationMapper {
    private static final String ORIGINAL_ANNOTATION = "org.eclipse.microprofile.metrics.annotation.SimplyTimed";
    private static final String NEW_ANNOTATION = "io.helidon.di.metrics.InternalSimplyTimed";
    private static final AnnotationValue<?> ANNOTATION = AnnotationValue.builder(NEW_ANNOTATION).build();

    @Override
    public String getName() {
        return ORIGINAL_ANNOTATION;
    }

    @Override
    public List<AnnotationValue<?>> map(AnnotationValue<Annotation> annotation, VisitorContext visitorContext) {
        return List.of(ANNOTATION);
    }
}
