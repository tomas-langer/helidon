package io.helidon.di.media;

import javax.inject.Singleton;

import io.helidon.config.Config;
import io.helidon.media.common.MediaSupport;
import io.helidon.media.jackson.JacksonSupport;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

@Factory
@Requires(classes = JacksonSupport.class)
public class Jackson {
    private final Config config;

    protected Jackson(Config config) {
        this.config = config;
    }

    @Singleton
    protected MediaSupport mediaSupport() {
        return JacksonSupport.create();
    }
}
