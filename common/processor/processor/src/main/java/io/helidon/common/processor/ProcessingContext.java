package io.helidon.common.processor;

import java.util.List;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;

import io.helidon.common.processor.spi.AnnotationMapper;
import io.helidon.common.processor.spi.ElementMapper;
import io.helidon.common.processor.spi.TypeMapper;
import io.helidon.common.types.TypeName;

public interface ProcessingContext {
    static ProcessingContext create(ProcessingEnvironment processingEnv) {
        return new ProcessingContextImpl(processingEnv);
    }

    ProcessingEnvironment aptEnv();
    AptOptions options();
    List<ElementMapper> elementMappers();
    List<TypeMapper> typeMappers();
    List<AnnotationMapper> annotationMappers();
    Set<TypeName> mapperSupportedAnnotations();

    /**
     * Packages supported by mapper providers.
     * @return
     */
    Set<String> mapperSupportedAnnotationPackages();
    Set<String> mapperSupportedOptions();
}
