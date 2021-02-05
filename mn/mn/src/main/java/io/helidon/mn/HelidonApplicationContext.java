package io.helidon.mn;

import java.util.function.Consumer;

import io.helidon.config.Config;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.context.ApplicationContextConfiguration;
import io.micronaut.context.DefaultApplicationContext;
import io.micronaut.context.DefaultApplicationContextBuilder;
import io.micronaut.context.env.Environment;

public class HelidonApplicationContext extends DefaultApplicationContext {
    HelidonApplicationContext(HelidonApplicationContextConfiguration config) {
        super(config);
    }

    public static Builder builder() {
        return new Builder();
    }

    @NonNull
    @Override
    protected Environment createEnvironment(@NonNull ApplicationContextConfiguration configuration) {
        return new HelidonEnvironment(configuration, ((HelidonApplicationContextConfiguration) configuration).config());
    }

    @Override
    protected void startEnvironment() {
        super.startEnvironment();
        HelidonEnvironment env = (HelidonEnvironment) getEnvironment();
        registerSingleton(env.getHelidonConfig());
    }



    public static class Builder extends DefaultApplicationContextBuilder implements HelidonApplicationContextConfiguration, io.helidon.common.Builder<HelidonApplicationContext> {
        private final Config.Builder config = Config.builder();

        @Override
        public HelidonApplicationContext build() {
            return new HelidonApplicationContext(this);
        }

        @NonNull
        @Override
        public Config.Builder config() {
            return config;
        }

        public Builder updateConfig(Consumer<Config.Builder> updater) {
            updater.accept(config);
            return this;
        }
    }
}
