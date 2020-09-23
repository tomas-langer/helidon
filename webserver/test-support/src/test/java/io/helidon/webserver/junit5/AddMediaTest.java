/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.webserver.junit5;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Single;
import io.helidon.media.common.ContentReaders;
import io.helidon.media.common.ContentWriters;
import io.helidon.media.common.MediaSupport;
import io.helidon.media.common.MessageBodyOperator;
import io.helidon.media.common.MessageBodyReader;
import io.helidon.media.common.MessageBodyReaderContext;
import io.helidon.media.common.MessageBodyWriter;
import io.helidon.media.common.MessageBodyWriterContext;
import io.helidon.webclient.WebClient;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@HelidonReactiveTest
@AddMedia(AddMediaTest.DataMedia.class)
class AddMediaTest {
    @AddRoute("/media")
    static void routeMedia(ServerRequest req, ServerResponse res) {
        res.send(new Data("Hi"));
    }

    @Test
    void testAddMedia(WebClient webClient) {
        Data await = webClient.get()
                .path("/media")
                .request(Data.class)
                .await(10, TimeUnit.SECONDS);

        assertThat(await.message(), is("Hi"));
    }

    private static class Data {
        private String Message;

        Data(String message) {
            Message = message;
        }

        String message() {
            return Message;
        }
    }

    static class DataMedia implements MediaSupport {
        @Override
        public Collection<MessageBodyReader<?>> readers() {
            return Set.of(new MessageBodyReader<Data>() {
                @Override
                public <U extends Data> Single<U> read(Flow.Publisher<DataChunk> publisher,
                                                       GenericType<U> type,
                                                       MessageBodyReaderContext context) {

                    return ContentReaders.readString(publisher, StandardCharsets.UTF_8)
                            .map(Data::new)
                            .map(type::cast);
                }

                @Override
                public PredicateResult accept(GenericType<?> type, MessageBodyReaderContext context) {
                    return DataMedia.accept(type);
                }
            });
        }

        @Override
        public Collection<MessageBodyWriter<?>> writers() {
            return Set.of(new MessageBodyWriter<Data>() {
                @Override
                public Flow.Publisher<DataChunk> write(Single<? extends Data> single,
                                                       GenericType<? extends Data> type,
                                                       MessageBodyWriterContext context) {
                    return single.flatMap(data -> ContentWriters.writeCharSequence(data.message(), StandardCharsets.UTF_8));
                }

                @Override
                public PredicateResult accept(GenericType<?> type, MessageBodyWriterContext context) {
                    return DataMedia.accept(type);
                }
            });
        }

        private static MessageBodyOperator.PredicateResult accept(GenericType<?> type) {
            return type.rawType().equals(Data.class)
                    ? MessageBodyOperator.PredicateResult.SUPPORTED
                    : MessageBodyOperator.PredicateResult.NOT_SUPPORTED;
        }
    }
}
