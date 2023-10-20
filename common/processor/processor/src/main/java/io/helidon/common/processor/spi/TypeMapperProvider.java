package io.helidon.common.processor.spi;

import javax.annotation.processing.ProcessingEnvironment;

import io.helidon.common.processor.AptOptions;

public interface TypeMapperProvider extends ProcessingProvider {
    TypeMapper create(ProcessingEnvironment aptEnv, AptOptions options);
}
