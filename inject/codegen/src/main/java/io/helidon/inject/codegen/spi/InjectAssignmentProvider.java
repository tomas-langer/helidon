package io.helidon.inject.codegen.spi;

import io.helidon.codegen.CodegenContext;

public interface InjectAssignmentProvider {
    ProviderSupport create(CodegenContext ctx);
}
