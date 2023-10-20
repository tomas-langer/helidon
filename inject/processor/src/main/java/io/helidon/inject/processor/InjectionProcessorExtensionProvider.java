package io.helidon.inject.processor;

import java.util.Collection;
import java.util.Set;

import io.helidon.common.types.TypeName;
import io.helidon.inject.processor.spi.HelidonProcessorExtension;
import io.helidon.inject.processor.spi.HelidonProcessorExtensionProvider;
import io.helidon.inject.tools.TypeNames;

public class InjectionProcessorExtensionProvider implements HelidonProcessorExtensionProvider {
    @Override
    public Collection<TypeName> supportedTypes() {
        return Set.of(TypeNames.JAKARTA_SINGLETON_TYPE,
                      TypeNames.INTERCEPTED_TYPE,
                      TypeNames.JAKARTA_INJECT_TYPE,
                      TypeNames.JAKARTA_PRE_DESTROY_TYPE,
                      TypeNames.JAKARTA_POST_CONSTRUCT_TYPE);
    }

    @Override
    public HelidonProcessorExtension create(InjectionProcessingContext ctx) {
        return new InjectionProcessorExtension(ctx);
    }
}
