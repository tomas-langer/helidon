package io.helidon.inject.processor.spi;

import java.util.Set;

import io.helidon.common.types.TypeName;

public interface InjectProcessorExtensionProvider {
    Set<TypeName> supportedTypes();
    Set<String> supportedOptions();
}
