package io.helidon.common.processor.spi;

import java.util.Collection;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;

import io.helidon.common.processor.AptOptions;
import io.helidon.common.types.TypeName;

public interface ProcessingProvider<T> {
    default Collection<String> supportedOptions() {
        return Set.of();
    }

    default Collection<TypeName> supportedTypes() {
        return Set.of();
    }

    /**
     * All annotations prefixed by this name will be included.
     *
     * @return collection of supported annotation packages
     */
    default Collection<String> supportedAnnotationPackages() {
        return Set.of();
    }

    T create(ProcessingEnvironment aptEnv, AptOptions options);
}
