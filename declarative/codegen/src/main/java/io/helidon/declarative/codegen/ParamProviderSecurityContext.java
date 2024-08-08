package io.helidon.declarative.codegen;

import java.util.NoSuchElementException;

import io.helidon.common.types.TypeName;
import io.helidon.declarative.codegen.spi.HttpParameterCodegenProvider;

class ParamProviderSecurityContext implements HttpParameterCodegenProvider {
    private static final TypeName COMMON_SECURITY_CONTEXT = TypeName.create("io.helidon.common.security.SecurityContext");
    private static final TypeName SECURITY_CONTEXT = TypeName.create("io.helidon.security.SecurityContext");

    @Override
    public boolean codegen(ParameterCodegenContext ctx) {
        if (!ctx.qualifiers().isEmpty()) {
            // any qualified instance is ignored
            return false;
        }
        TypeName typeName = ctx.parameterType();
        if (COMMON_SECURITY_CONTEXT.equals(typeName) || SECURITY_CONTEXT.equals(typeName)) {
            codegenSecurityContext(ctx, typeName);
            return true;
        }
        return false;
    }

    private void codegenSecurityContext(ParameterCodegenContext ctx, TypeName typeName) {
        String param = ctx.paramName();

        ctx.contentBuilder()
                .addContentLine(ctx.serverRequestParamName())
                .increaseContentPadding()
                .increaseContentPadding()
                .addContentLine(".context()")
                .addContent(".get(")
                .addContent(typeName)
                .addContentLine(".class)")
                .addContent(".orElseThrow(() -> new ")
                .addContent(NoSuchElementException.class)
                .addContent("(\"")
                .addContent(typeName)
                .addContent(" is not present in request context, required for parameter ")
                .addContent(param)
                .addContentLine(". Maybe security is not configured?\"));")
                .decreaseContentPadding()
                .decreaseContentPadding();
    }
}
