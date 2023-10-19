package io.helidon.common.processor.spi;

import java.util.Collection;

import io.helidon.common.processor.ProcessingContext;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.ElementKind;

/**
 * Maps an annotation to zero or more annotations.
 */
public interface AnnotationMapper {
    /**
     * Predicate to filter annotations that should be passed to this mapper.
     *
     * @return predicate
     */
    boolean supportsAnnotation(Annotation annotation);

    /**
     * Map an annotation to a set of new annotations.
     * The original annotation is not retained, unless part of the result of this method.
     *
     * @param ctx processing context
     * @param original original annotation that matches {@link #supportsAnnotation()}
     * @param elementKind kind of element the annotation is on
     * @return list of annotations to add instead of the provided annotation (may be empty to remove it),
     *          this result is used to process other mappers (except for this one)
     */
    Collection<Annotation> mapAnnotation(ProcessingContext ctx, Annotation original, ElementKind elementKind);
}
