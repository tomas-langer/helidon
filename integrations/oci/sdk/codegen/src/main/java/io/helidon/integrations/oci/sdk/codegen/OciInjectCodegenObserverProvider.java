package io.helidon.integrations.oci.sdk.codegen;

import java.util.Set;
import java.util.function.Function;

import io.helidon.codegen.Option;
import io.helidon.common.GenericType;
import io.helidon.inject.codegen.InjectionCodegenContext;
import io.helidon.inject.codegen.spi.InjectCodegenObserver;
import io.helidon.inject.codegen.spi.InjectCodegenObserverProvider;

public class OciInjectCodegenObserverProvider implements InjectCodegenObserverProvider {
    static final Option<Set<String>> OPTION_TYPENAME_EXCEPTIONS =
            Option.createSet("inject.oci.codegenExclusions",
                             "Set of type to exclude from generation",
                             Set.of(),
                             Function.identity(),
                             new GenericType<Set<String>>() { });
    static final Option<Set<String>> OPTION_NO_DOT_EXCEPTIONS =
            Option.createSet("inject.oci.builderNameExceptions",
                             "Set of types that do not use a dot between type and builder.",
                             Set.of(),
                             Function.identity(),
                             new GenericType<Set<String>>() { });

    /**
     * Service loader based constructor.
     *
     * @deprecated this is a Java ServiceLoader implementation and the constructor should not be used directly
     */
    @Deprecated
    public OciInjectCodegenObserverProvider() {
        super();
    }

    @Override
    public InjectCodegenObserver create(InjectionCodegenContext context) {
        return new OciInjectionCodegenObserver(context);
    }

    @Override
    public Set<Option<?>> supportedOptions() {
        return Set.of(OPTION_NO_DOT_EXCEPTIONS,
                      OPTION_TYPENAME_EXCEPTIONS);
    }
}
