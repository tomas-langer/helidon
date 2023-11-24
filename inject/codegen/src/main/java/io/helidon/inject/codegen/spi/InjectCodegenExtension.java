package io.helidon.inject.codegen.spi;

import io.helidon.inject.codegen.RoundContext;

public interface InjectCodegenExtension {
    /**
     * Process a single round.
     *
     * @param roundContext round context
     */
    void process(RoundContext roundContext);

    default void processingOver() {
    }
}
