package io.helidon.config.metadata.codegen;

import java.util.Set;

import io.helidon.codegen.CodegenContext;
import io.helidon.codegen.spi.CodegenExtension;
import io.helidon.codegen.spi.CodegenExtensionProvider;
import io.helidon.common.types.TypeName;

import static io.helidon.config.metadata.codegen.ConfigMetadataTypes.CONFIGURED;
import static io.helidon.config.metadata.codegen.ConfigMetadataTypes.META_CONFIGURED;
import static io.helidon.config.metadata.codegen.ConfigMetadataTypes.META_OPTION;
import static io.helidon.config.metadata.codegen.ConfigMetadataTypes.META_OPTIONS;

public class ConfigMetadataCodegenProvider implements CodegenExtensionProvider {
    public ConfigMetadataCodegenProvider() {
    }

    @Override
    public CodegenExtension create(CodegenContext ctx, TypeName generatorType) {
        return new ConfigMetadataCodegenExtension(ctx);
    }

    @Override
    public Set<TypeName> supportedAnnotations() {
        return Set.of(META_CONFIGURED,
                      META_OPTION,
                      META_OPTIONS,
                      CONFIGURED);
    }
}
