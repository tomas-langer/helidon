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

import java.util.List;

import io.helidon.config.Config;
import io.helidon.media.common.MediaContext;
import io.helidon.media.common.MessageBodyReader;
import io.helidon.media.common.MessageBodyStreamReader;
import io.helidon.media.common.MessageBodyStreamWriter;
import io.helidon.media.common.MessageBodyWriter;

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;

/**
 * Configurations Media support.
 */
@ConfigurationProperties(MediaContextConfiguration.PREFIX)
public class MediaContextConfiguration {
    public static final String PREFIX = "media";
    @ConfigurationBuilder(prefixes = "")
    final MediaContext.Builder builder;
    private final List<MessageBodyReader<?>> messageBodyReaders;
    private final List<MessageBodyWriter<?>> messageBodyWriters;
    private final List<MessageBodyStreamReader<?>> streamReaders;
    private final List<MessageBodyStreamWriter<?>> streamWriters;

    /**
     * Constructs the server configuration.
     * @param config               The helidon config
     * @param messageBodyReaders   The message body readers
     * @param messageBodyWriters   The message body writers
     * @param streamReaders        The stream readers
     * @param streamWriters        The stream writers
     */
    public MediaContextConfiguration(Config config,
                                     List<MessageBodyReader<?>> messageBodyReaders,
                                     List<MessageBodyWriter<?>> messageBodyWriters,
                                     List<MessageBodyStreamReader<?>> streamReaders,
                                     List<MessageBodyStreamWriter<?>> streamWriters) {
        Config mediaContextConfig = config.get(PREFIX);
        this.builder = MediaContext.builder()
                .registerDefaults(config.get("register-defaults").asBoolean().orElse(true))
                .config(mediaContextConfig);
        this.messageBodyReaders = messageBodyReaders;
        this.messageBodyWriters = messageBodyWriters;
        this.streamReaders = streamReaders;
        this.streamWriters = streamWriters;
    }

    /**
     * @return The builder.
     */
    public MediaContext.Builder getBuilder() {
        return builder;
    }

    /**
     * @return The message body readers
     */
    public List<MessageBodyReader<?>> getMessageBodyReaders() {
        return messageBodyReaders;
    }

    /**
     * @return The message body writers
     */
    public List<MessageBodyWriter<?>> getMessageBodyWriters() {
        return messageBodyWriters;
    }

    /**
     * @return The stream readers
     */
    public List<MessageBodyStreamReader<?>> getStreamReaders() {
        return streamReaders;
    }

    /**
     * @return The stream writers
     */
    public List<MessageBodyStreamWriter<?>> getStreamWriters() {
        return streamWriters;
    }
}
