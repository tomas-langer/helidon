package io.helidon.common.processor.spi;

import javax.annotation.processing.ProcessingEnvironment;

import io.helidon.common.processor.AptOptions;

public interface AnnotationMapperProvider extends ProcessingProvider {
    AnnotationMapper create(ProcessingEnvironment aptEnv, AptOptions options);
}
