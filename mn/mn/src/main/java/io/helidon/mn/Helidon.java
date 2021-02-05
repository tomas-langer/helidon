package io.helidon.mn;

import java.util.function.Consumer;

import io.helidon.common.LogConfig;
import io.helidon.config.Config;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.ApplicationContextBuilder;
import io.micronaut.context.DefaultApplicationContextBuilder;

public class Helidon extends DefaultApplicationContextBuilder
        implements ApplicationContextBuilder, HelidonApplicationContextConfiguration {
    private final Config.Builder config = Config.builder();

    protected Helidon() {
    }

    public static ApplicationContext run() {
        return new Helidon()
                .start();
    }

    public Helidon updateConfig(Consumer<Config.Builder> updater) {
        updater.accept(config);
        return this;
    }

    @NonNull
    @Override
    protected ApplicationContext newApplicationContext() {
        return new HelidonApplicationContext(this);
    }

    @NonNull
    @Override
    public Config.Builder config() {
        return config;
    }

    @NonNull
    @Override
    public ApplicationContext start() {
        LogConfig.configureRuntime();
        ApplicationContext applicationContext = super.build();
        applicationContext.start();

        applicationContext.publishEvent(new ContextStartedEvent(this));
        return applicationContext;
    }
}
