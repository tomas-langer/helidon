package io.helidon.declarative.codegen;

import io.helidon.common.types.TypeName;

class ParamProviderContext implements io.helidon.declarative.codegen.spi.HttpParameterCodegenProvider {
    private static final TypeName CONTEXT = TypeName.create("io.helidon.common.context.Context");

    @Override
    public boolean codegen(ParameterCodegenContext ctx) {
        if (!ctx.qualifiers().isEmpty()) {
            // any qualified instance is ignored
            return false;
        }

        if (CONTEXT.equals(ctx.parameterType())) {
            codegenContext(ctx);
            return true;
        }
        return false;
    }

    private void codegenContext(ParameterCodegenContext ctx) {
        ctx.contentBuilder()
                .addContent(ctx.serverRequestParamName())
                .addContent(".context();");
    }
}
