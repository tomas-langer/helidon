package io.helidon.common.processor.spi;

import javax.annotation.processing.ProcessingEnvironment;

import io.helidon.common.processor.AptOptions;

public interface ElementMapperProvider extends ProcessingProvider {
    ElementMapper create(ProcessingEnvironment aptEnv, AptOptions options);
}
