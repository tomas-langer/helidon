package io.helidon.inject.codegen.spi;

import java.util.Set;

import io.helidon.codegen.Option;
import io.helidon.inject.codegen.InjectionCodegenContext;

/**
 * A {@link java.util.ServiceLoader} provider interface for observers that will be
 * called for code generation events.
 */
public interface InjectCodegenObserverProvider {
    /**
     * The provider can add supported options.
     *
     * @return options supported by this provider
     */
    default Set<Option<?>> supportedOptions() {
        return Set.of();
    }

    /**
     * Create a new observer based on the Helidon Inject code generation context.
     *
     * @param context code generation context for this code generation session
     * @return a new observer
     */
    InjectCodegenObserver create(InjectionCodegenContext context);
}
