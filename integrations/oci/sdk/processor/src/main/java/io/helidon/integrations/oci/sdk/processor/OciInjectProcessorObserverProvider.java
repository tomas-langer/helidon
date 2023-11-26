package io.helidon.integrations.oci.sdk.processor;

import java.util.Set;

import io.helidon.inject.codegen.InjectionCodegenContext;
import io.helidon.inject.codegen.spi.InjectCodegenObserver;
import io.helidon.inject.codegen.spi.InjectCodegenObserverProvider;

public class OciInjectProcessorObserverProvider implements InjectCodegenObserverProvider {
    static final String OPTION_TYPENAME_EXCEPTIONS = "inject.oci.codegenExclusions";
    static final String OPTION_NO_DOT_EXCEPTIONS = "inject.oci.builderNameExceptions";

    /**
     * Service loader based constructor.
     *
     * @deprecated this is a Java ServiceLoader implementation and the constructor should not be used directly
     */
    @Deprecated
    public OciInjectProcessorObserverProvider() {
        super();
    }

    @Override
    public InjectCodegenObserver create(InjectionCodegenContext context) {
        return new OciInjectionProcessorObserver(context);
    }

    @Override
    public Set<String> supportedOptions() {
        return Set.of(OPTION_NO_DOT_EXCEPTIONS,
                      OPTION_TYPENAME_EXCEPTIONS);
    }
}
