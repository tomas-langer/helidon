package io.helidon.di.media;

import javax.inject.Singleton;

import io.helidon.config.Config;
import io.helidon.media.common.MediaSupport;
import io.helidon.media.jsonb.JsonbSupport;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

@Factory
@Requires(beans = javax.json.bind.Jsonb.class)
@Requires(classes = JsonbSupport.class)
public class Jsonb {
    private final Config config;

    protected Jsonb(Config config) {
        this.config = config;
    }

    /**
     * The MediaSupport for JSON-B.
     * @param jsonb The json b object
     * @return The MediaSupport
     */
    @Singleton
    protected MediaSupport mediaSupport(javax.json.bind.Jsonb jsonb) {
        return JsonbSupport.create(jsonb);
    }
}
