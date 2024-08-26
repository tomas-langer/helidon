/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

package io.helidon.microprofile.tests.server;

import java.nio.charset.StandardCharsets;

import io.helidon.microprofile.testing.junit5.AddBean;
import io.helidon.microprofile.testing.junit5.HelidonTest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@HelidonTest
@AddBean(SmallEntityBufferingTest.StreamResource.class)
public class SmallEntityBufferingTest {
    private static final String RESPONSE = "Small data.";

    @Test
    public void smallEntityUsesContentLength(WebTarget target) throws Exception {
        Response response = target.path("/test/stream")
                .request(MediaType.TEXT_PLAIN)
                .get();

        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(String.class), is(RESPONSE));
        // must be content-length, not chunked, as we are expecting to buffer smaller entities
        assertThat(response.getHeaderString("Transfer-Encoding"), nullValue());
        assertThat(response.getHeaderString("Content-Length"), is(RESPONSE.length()));
    }

    @Path("/test")
    @ApplicationScoped
    public static class StreamResource {

        @GET
        @Path(value = "/stream")
        @Produces(MediaType.TEXT_PLAIN)
        public Response stream() {

            StreamingOutput stream = output -> {
                output.write(RESPONSE.getBytes(StandardCharsets.UTF_8));
            };
            return Response.ok(stream).build();
        }
    }
}
