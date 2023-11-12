package io.helidon.inject.configdriven.processor;

import java.util.Collection;
import java.util.Set;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.common.types.TypeName;
import io.helidon.inject.processor.InjectionProcessingContext;
import io.helidon.inject.processor.spi.HelidonProcessorExtension;
import io.helidon.inject.processor.spi.HelidonProcessorExtensionProvider;

@Weight(Weighted.DEFAULT_WEIGHT - 10)
public class ConfigDrivenProcessorExtensionProvider implements HelidonProcessorExtensionProvider {
    @Override
    public Collection<TypeName> supportedTypes() {
        return Set.of(ConfigDrivenAnnotation.TYPE);
    }

    @Override
    public HelidonProcessorExtension create(InjectionProcessingContext ctx) {
        return new ConfigDrivenProcessorExtension(ctx);
    }
}
