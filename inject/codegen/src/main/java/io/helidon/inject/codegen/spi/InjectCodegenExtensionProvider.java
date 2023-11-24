package io.helidon.inject.codegen.spi;

import io.helidon.codegen.spi.CodegenProvider;
import io.helidon.inject.codegen.InjectionCodegenContext;

public interface InjectCodegenExtensionProvider extends CodegenProvider {
    InjectCodegenExtension create(InjectionCodegenContext codegenContext);
}
