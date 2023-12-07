package io.helidon.inject.codegen.spi;

import java.util.Set;

import io.helidon.codegen.Option;
import io.helidon.inject.codegen.InjectionCodegenContext;

/**
 * Implementations of these are service-loaded by the {@link java.util.ServiceLoader}, and will be
 * called to be able to observe processing events.
 */
public interface InjectCodegenObserverProvider {
    default Set<Option<?>> supportedOptions() {
        return Set.of();
    }

    InjectCodegenObserver create(InjectionCodegenContext context);
}
