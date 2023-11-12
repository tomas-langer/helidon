package io.helidon.inject.configdriven.configuredby.application.test;

import java.util.List;
import java.util.Optional;

import io.helidon.config.ConfigProducer__ServiceDescriptor;
import io.helidon.inject.api.Application;
import io.helidon.inject.api.ServiceInjectionPlanBinder;
import io.helidon.inject.configdriven.configuredby.test.ASingletonService__ServiceDescriptor;
import io.helidon.inject.configdriven.configuredby.test.SomeConfiguredServiceWithAnAbstractBase__ServiceDescriptor;
import io.helidon.inject.configdriven.configuredby.test.SomeServiceConfig;
import io.helidon.inject.configdriven.tests.config.FakeWebServerNotDrivenAndHavingConfiguredByOverrides__ServiceDescriptor;

public class HelidonInjection__Application_Test implements Application {
    private static final String NAME = "io.helidon.inject.configdriven.configuredby.application.test";

    /**
     * Constructor for {@link java.util.ServiceLoader}.
     *
     * @deprecated used by {@link java.util.ServiceLoader}, do not use directly
     */
    @Deprecated
    public HelidonInjection__Application_Test() {

    }

    @Override
    public Optional<String> named() {
        return Optional.of(NAME);
    }

    @Override
    public void configure(ServiceInjectionPlanBinder binder) {
        // io.helidon.config
        // io.helidon.config.ConfigProducer
        binder.bindTo(ConfigProducer__ServiceDescriptor.INSTANCE)
                .bindMany(ConfigProducer__ServiceDescriptor.IP_PARAM_0, List.of())
                .commit();

        // io.helidon.inject.configdriven.configuredby.application.test
        // io.helidon.inject.configdriven.configuredby.application.test.ASimpleRunLevelService
        binder.bindTo(ASimpleRunLevelService__ServiceDescriptor.INSTANCE)
                .bind(ASimpleRunLevelService__ServiceDescriptor.IP_PARAM_0, ASingletonService__ServiceDescriptor.INSTANCE)
                .bindMany(ASimpleRunLevelService__ServiceDescriptor.IP_PARAM_0,
                          List.of(FakeWebServerNotDrivenAndHavingConfiguredByOverrides__ServiceDescriptor.INSTANCE))
                .commit();

        // unknown/io.helidon.inject.configdriven.configuredby.test.
        // io.helidon.inject.configdriven.configuredby.test.ASingletonService
        binder.bindTo(ASingletonService__ServiceDescriptor.INSTANCE)
                .commit();

        // ""
        // io.helidon.inject.configdriven.configuredby.test.SomeConfiguredServiceWithAnAbstractBase
        binder.bindTo(SomeConfiguredServiceWithAnAbstractBase__ServiceDescriptor.INSTANCE)
                // used to be resolveBind
                .runtimeBind(SomeConfiguredServiceWithAnAbstractBase__ServiceDescriptor.IP_PARAM_0,
                              SomeServiceConfig.class)
                .commit();

        binder.bindTo(io.helidon.inject.configdriven.tests.config.FakeWebServerNotDrivenAndHavingConfiguredByOverrides$$Injection$$Activator.INSTANCE)
                .runtimeBind("io.helidon.inject.configdriven.tests.config.<init>|2(1)", io.helidon.inject.configdriven.tests.config.FakeServerConfig.class)
                .bindVoid("io.helidon.inject.configdriven.tests.config.<init>|2(2)")
                .commit();
    }

    @Override
    public String toString() {
        return NAME + ":" + getClass().getName();
    }
}
