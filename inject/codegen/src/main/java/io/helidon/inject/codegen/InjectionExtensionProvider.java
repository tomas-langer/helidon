package io.helidon.inject.codegen;

import java.util.Set;

import io.helidon.codegen.Option;
import io.helidon.common.types.TypeName;
import io.helidon.inject.codegen.spi.InjectCodegenExtension;
import io.helidon.inject.codegen.spi.InjectCodegenExtensionProvider;

public class InjectionExtensionProvider implements InjectCodegenExtensionProvider {
    @Override
    public Set<Option<?>> supportedOptions() {
        return Set.of(InjectOptions.AUTO_ADD_NON_CONTRACT_INTERFACES,
                      InjectOptions.INTERCEPTION_STRATEGY,
                      InjectOptions.SCOPE_META_ANNOTATIONS);
    }

    @Override
    public Set<TypeName> supportedAnnotations() {
        return Set.of(InjectCodegenTypes.INJECT_SINGLETON,
                      InjectCodegenTypes.INJECT_PRE_DESTROY,
                      InjectCodegenTypes.INJECT_POST_CONSTRUCT,
                      InjectCodegenTypes.INJECT_INJECT,
                      InjectCodegenTypes.HELIDON_INTERCEPTED,
                      InjectCodegenTypes.HELIDON_DESCRIBE);
    }

    @Override
    public InjectCodegenExtension create(InjectionCodegenContext codegenContext) {
        return new InjectionExtension(codegenContext);
    }
}
