package io.helidon.inject.processor.spi;

import javax.annotation.processing.RoundEnvironment;

import io.helidon.inject.processor.RoundContext;

public interface InjectProcessorExtension {
    boolean process(RoundContext context);

    default void process(RoundEnvironment roundEnvironment) {
    }

    default void processingOver(RoundEnvironment roundEnvironment) {
    }
}
