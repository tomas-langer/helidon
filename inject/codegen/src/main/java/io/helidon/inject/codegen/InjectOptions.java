package io.helidon.inject.codegen;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.codegen.CodegenOptions;
import io.helidon.common.types.TypeName;

/**
 * Supported options specific to Helidon Inject.
 */
public final class InjectOptions {
    /**
     * Treat all super types as a contract for a given service type being added.
     */
    public static final String AUTO_ADD_NON_CONTRACT_INTERFACES = "inject.autoAddNonContractInterfaces";
    /**
     * Which {@code io.helidon.inject.api.InterceptionStrategy} to use.
     */
    public static final String INTERCEPTION_STRATEGY = "inject.interceptionStrategy";
    /**
     * Additional meta annotations that mark scope annotations. This can be used to include
     * jakarta.enterprise.context.NormalScope annotated types as scopes.
     */
    public static final String SCOPE_META_ANNOTATIONS = "inject.scopeMetaAnnotations";
    /**
     * Identify whether any application scopes (from ee) is translated to {@code jakarta.inject.Singleton}.
     */
    public static final String MAP_APPLICATION_TO_SINGLETON_SCOPE = "inject.mapApplicationToSingletonScope";
    /**
     * Identify whether any unsupported types should trigger annotation processing to keep going (the default is to fail).
     */
    public static final String IGNORE_UNSUPPORTED_ANNOTATIONS = "inject.ignoreUnsupportedAnnotations";
    public static final String JSR_330_STRICT = "inject.supports-jsr330.strict";
    public static final String MODULE_NAME = "inject.module-name";

    private InjectOptions() {
    }

    public static InterceptionStrategy interceptionStrategy(CodegenOptions options) {
        return options.option(INTERCEPTION_STRATEGY, InterceptionStrategy.EXPLICIT, InterceptionStrategy.class);
    }

    public static Set<TypeName> scopeMetaAnnotations(CodegenOptions options) {
        return Stream.concat(options.asList(SCOPE_META_ANNOTATIONS)
                                     .stream()
                                     .map(TypeName::create),
                             Stream.of(InjectCodegenTypes.INJECT_SCOPE))
                .collect(Collectors.toSet());
    }

    static boolean autoAddNonContractInterfaces(CodegenOptions options) {
        return options.option(AUTO_ADD_NON_CONTRACT_INTERFACES, false);
    }
}
