/*
 * Copyright (c) 2023 Oracle and/or its affiliates.
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
 * HTTP SSE common package. Shares types between webclient and webserver modules.
 */
module io.helidon.http.sse {
    requires transitive io.helidon.common;
    requires transitive io.helidon.common.media.type;
    requires io.helidon.http.media;
    requires io.helidon.http;

    exports io.helidon.http.sse;
}