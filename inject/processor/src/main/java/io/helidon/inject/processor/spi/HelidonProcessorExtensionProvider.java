package io.helidon.inject.processor.spi;

import io.helidon.common.processor.spi.ProcessingProvider;
import io.helidon.inject.processor.InjectionProcessingContext;

public interface HelidonProcessorExtensionProvider extends ProcessingProvider {
    HelidonProcessorExtension create(InjectionProcessingContext ctx);

}
