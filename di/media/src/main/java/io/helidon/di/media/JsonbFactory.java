/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.di.media;

import javax.inject.Singleton;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;

/**
 * Configures JSON-B if not already present.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@Factory
@Requires(classes = Jsonb.class)
public class JsonbFactory {

    private final JsonbConfiguration configuration;

    /**
     * @param configuration The configuration.
     */
    public JsonbFactory(JsonbConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * @return The JSON-B config
     */
    @Singleton
    protected JsonbConfig jsonbConfig() {
        return configuration.getJsonbConfig();
    }

    /**
     * @param config The JSON-B config
     * @return The JSON-B instance.
     */
    @Singleton
    @Requires(missingBeans = Jsonb.class)
    protected Jsonb jsonb(JsonbConfig config) {
        return JsonbBuilder.create(config);
    }
}
