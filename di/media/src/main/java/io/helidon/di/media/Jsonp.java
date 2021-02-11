package io.helidon.di.media;

import java.util.Map;

import javax.inject.Singleton;
import javax.json.JsonObject;

import io.helidon.config.Config;
import io.helidon.media.common.MediaSupport;
import io.helidon.media.jsonp.JsonpSupport;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

@Factory
@Requires(classes = {JsonpSupport.class, JsonObject.class})
public class Jsonp {
    private final Config config;

    protected Jsonp(Config config) {
        this.config = config;
    }

    @Singleton
    protected MediaSupport mediaSupport() {
        return JsonpSupport.create(config.get("media.jsonp").asMap().orElseGet(Map::of));
    }
}
