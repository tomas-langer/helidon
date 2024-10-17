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

module io.helidon.service.tests.inject.toolbox {
    requires io.helidon.service.registry;
    requires io.helidon.service.inject.api;

    // we use Application
    requires io.helidon.service.inject;
    requires io.helidon.config;
    requires io.helidon.config.yaml;
    requires io.helidon.http;
    requires io.helidon.common.context;
    requires io.helidon.metrics.api;

    exports io.helidon.service.tests.inject.toolbox;
    exports io.helidon.service.tests.inject.toolbox.impl to io.helidon.service.registry;
}