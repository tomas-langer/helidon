package io.helidon.inject.codegen.spi;

import io.helidon.inject.codegen.RoundContext;

/**
 * Code generation extension for Helidon Inject.
 */
public interface InjectCodegenExtension {
    /**
     * Process a single round.
     *
     * @param roundContext round context
     */
    void process(RoundContext roundContext);

    /**
     * Called when the processing is over, and there will not be an additional processing round.
     */
    default void processingOver() {
    }
}
