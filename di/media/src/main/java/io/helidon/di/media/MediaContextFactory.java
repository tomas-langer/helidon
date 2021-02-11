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

import javax.inject.Singleton;

import io.helidon.media.common.MediaContext;
import io.helidon.media.common.MediaSupport;
import io.helidon.media.common.MessageBodyReader;
import io.helidon.media.common.MessageBodyStreamReader;
import io.helidon.media.common.MessageBodyStreamWriter;
import io.helidon.media.common.MessageBodyWriter;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.core.util.CollectionUtils;

/**
 * Factory for default media support configuration.
 *
 * @author graemerocher
 * @since 1.0.0
 */
@Factory
public class MediaContextFactory {

    /**
     * Creates the MediaContext builder.
     *
     * @param configuration The configuration
     * @param mediaSupport  The media support
     * @return The media context builder
     */
    @Singleton
    @Primary
    protected MediaContext.Builder defaultMediaContextBuilder(
            MediaContextConfiguration configuration,
            List<MediaSupport> mediaSupport) {
        MediaContext.Builder builder = configuration.getBuilder();
        for (MessageBodyReader<?> messageBodyReader : configuration.getMessageBodyReaders()) {
            builder.addReader(messageBodyReader);
        }
        for (MessageBodyWriter<?> messageBodyWriter : configuration.getMessageBodyWriters()) {
            builder.addWriter(messageBodyWriter);
        }
        for (MessageBodyStreamReader<?> streamReader : configuration.getStreamReaders()) {
            builder.addStreamReader(streamReader);
        }
        for (MessageBodyStreamWriter<?> streamWriter : configuration.getStreamWriters()) {
            builder.addStreamWriter(streamWriter);
        }
        if (CollectionUtils.isNotEmpty(mediaSupport)) {
            mediaSupport.forEach(builder::addMediaSupport);
        }
        return builder;
    }

    /**
     * Creates the MediaContext.
     *
     * @param builder The media context builder
     * @return The media context
     */
    @Singleton
    @Primary
    protected MediaContext defaultMediaContext(MediaContext.Builder builder) {
        return builder.build();
    }
}
