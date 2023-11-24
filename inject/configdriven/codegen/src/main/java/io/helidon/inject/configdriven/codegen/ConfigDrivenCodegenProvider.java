package io.helidon.inject.configdriven.codegen;

import java.util.Set;

import io.helidon.common.Weight;
import io.helidon.common.Weighted;
import io.helidon.common.types.TypeName;
import io.helidon.inject.codegen.InjectionCodegenContext;
import io.helidon.inject.codegen.spi.InjectCodegenExtension;
import io.helidon.inject.codegen.spi.InjectCodegenExtensionProvider;

@Weight(Weighted.DEFAULT_WEIGHT - 10)
public class ConfigDrivenCodegenProvider implements InjectCodegenExtensionProvider {
    @Override
    public Set<TypeName> supportedAnnotations() {
        return Set.of(ConfigDrivenAnnotation.TYPE);
    }

    @Override
    public InjectCodegenExtension create(InjectionCodegenContext codegenContext) {
        return new ConfigDrivenCodegen(codegenContext);
    }
}
