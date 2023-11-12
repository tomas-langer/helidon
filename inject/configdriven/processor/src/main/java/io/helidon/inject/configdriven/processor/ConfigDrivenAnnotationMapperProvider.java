package io.helidon.inject.configdriven.processor;

import javax.annotation.processing.ProcessingEnvironment;

import io.helidon.common.processor.AptOptions;
import io.helidon.common.processor.spi.AnnotationMapper;
import io.helidon.common.processor.spi.AnnotationMapperProvider;

public class ConfigDrivenAnnotationMapperProvider implements AnnotationMapperProvider {
    @Override
    public AnnotationMapper create(ProcessingEnvironment aptEnv, AptOptions options) {
        return new ConfigDrivenAnnotationMapper();
    }
}
