package io.helidon.inject.codegen;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.helidon.codegen.CodegenOptions;
import io.helidon.codegen.Option;
import io.helidon.common.GenericType;
import io.helidon.common.types.TypeName;

/**
 * Supported options specific to Helidon Inject.
 */
public final class InjectOptions {
    /**
     * Treat all super types as a contract for a given service type being added.
     */
    public static final Option<Boolean> AUTO_ADD_NON_CONTRACT_INTERFACES =
            Option.create("inject.autoAddNonContractInterfaces",
                          "Treat all super types as a contract for a given service type being added.",
                          false);
    /**
     * Which {@code io.helidon.inject.api.InterceptionStrategy} to use.
     */
    public static final Option<InterceptionStrategy> INTERCEPTION_STRATEGY =
            Option.create("inject.interceptionStrategy",
                          "Which interception strategy to use (NONE, EXPLICIT, ALL_RUNTIME, ALL_RETAINED)",
                          InterceptionStrategy.EXPLICIT,
                          InterceptionStrategy::valueOf,
                          GenericType.create(InterceptionStrategy.class));

    /**
     * Additional meta annotations that mark scope annotations. This can be used to include
     * jakarta.enterprise.context.NormalScope annotated types as scopes.
     */
    public static final Option<Set<TypeName>> SCOPE_META_ANNOTATIONS =
            Option.createSet("inject.scopeMetaAnnotations",
                             "Additional meta annotations that mark scope annotations. This can be used to include"
                                     + "jakarta.enterprise.context.NormalScope annotated types as scopes.",
                             Set.of(),
                             TypeName::create,
                             new GenericType<Set<TypeName>>() { });
    /**
     * Identify whether any application scopes (from ee) is translated to {@code jakarta.inject.Singleton}.
     */
    public static final Option<Boolean> MAP_APPLICATION_TO_SINGLETON_SCOPE
            = Option.create("inject.mapApplicationToSingletonScope",
                            "Should we map application scoped beans from Jakarta CDI to Singleton services?",
                            false);
    /**
     * Identify whether any unsupported types should trigger annotation processing to keep going.
     */
    public static final Option<Boolean> IGNORE_UNSUPPORTED_ANNOTATIONS = Option.create(
            "inject.ignoreUnsupportedAnnotations",
            "Identify whether any unsupported types should trigger annotation processing to keep going.",
            false);
    public static final Option<Boolean> JSR_330_STRICT = Option.create(
            "inject.supports-jsr330.strict",
            "Use JSR-330 strict analysis of types (such as adding POJO if used for injection)",
            false);

    private InjectOptions() {
    }

    static Set<TypeName> scopeMetaAnnotations(CodegenOptions options) {
        return Stream.concat(SCOPE_META_ANNOTATIONS.value(options)
                                     .stream(),
                             Stream.of(InjectCodegenTypes.INJECT_SCOPE))
                .collect(Collectors.toSet());
    }
}
