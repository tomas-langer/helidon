package io.helidon.inject.codegen.jakarta;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.helidon.common.types.TypeName;
import io.helidon.inject.codegen.InjectCodegenTypes;
import io.helidon.inject.codegen.InjectOptions;
import io.helidon.inject.codegen.InjectionCodegenContext;
import io.helidon.inject.codegen.RoundContext;
import io.helidon.inject.codegen.spi.InjectCodegenExtension;

class UnsupportedTypesExtension implements InjectCodegenExtension {
    private final InjectionCodegenContext ctx;

    UnsupportedTypesExtension(InjectionCodegenContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void process(RoundContext roundContext) {
        Collection<TypeName> annotations = roundContext.availableAnnotations();

        if (annotations.isEmpty()) {
            // everything is fine, there is no unexpected annotation in the code we are processing
            return;
        }

        Set<TypeName> toProcess = new HashSet<>(annotations);

        if (ctx.options().enabled(MapApplicationScopedProvider.MAP_APPLICATION_TO_SINGLETON_SCOPE)) {
            toProcess.remove(CdiTypes.APPLICATION_SCOPED);

            if (toProcess.isEmpty()) {
                // only application scoped, and that was supported
                return;
            }
        }

        String unsupported = toProcess.stream()
                .map(TypeName::fqName)
                .collect(Collectors.joining(", "));

        if (InjectOptions.IGNORE_UNSUPPORTED_ANNOTATIONS.value(ctx.options())) {
            // not interested
            ctx.logger().log(System.Logger.Level.TRACE, "Ignoring unsupported annotations: " + unsupported);
            return;
        }

        StringBuilder msg = new StringBuilder("This module contains unsupported annotations for Injection to process: ")
                .append(unsupported)
                .append('\n');

        if (toProcess.contains(CdiTypes.APPLICATION_SCOPED)) {
            msg.append("'")
                    .append(CdiTypes.APPLICATION_SCOPED.fqName())
                    .append("' can be optionally mapped to '")
                    .append(InjectCodegenTypes.INJECT_POINT.fqName())
                    .append("' scope by passing -A")
                    .append(MapApplicationScopedProvider.MAP_APPLICATION_TO_SINGLETON_SCOPE)
                    .append("=true.\n");
        }

        msg.append("Use -A")
                .append(InjectOptions.IGNORE_UNSUPPORTED_ANNOTATIONS)
                .append("=true to ignore all unsupported annotations.\n");

        ctx.logger().log(System.Logger.Level.ERROR, msg.toString());
    }
}
