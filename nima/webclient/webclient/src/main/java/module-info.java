/*
 * Copyright (c) 2022 Oracle and/or its affiliates.
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

/**
 * WebClient API and HTTP/1.1 implementation.
 */
module io.helidon.nima.webclient {
    requires transitive io.helidon.common.uri;
    requires transitive io.helidon.nima.common.tls;
    requires transitive io.helidon.common.socket;
    requires transitive io.helidon.common.http;
    requires transitive io.helidon.nima.http.encoding;
    requires transitive io.helidon.nima.http.media;

    exports io.helidon.nima.webclient;
    exports io.helidon.nima.webclient.spi;
    /*
     This module exposes two packages, as we (want to) have cyclic dependency.
     The WebClient should support HTTP/1.1 out of the box and to have it in API, we must have the
     implementation available. The HTTP/1.1 client then implements the WebClient API...
     */
    exports io.helidon.nima.webclient.http1;
}