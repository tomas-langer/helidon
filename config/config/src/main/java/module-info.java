/*
 * Copyright (c) 2017, 2023 Oracle and/or its affiliates.
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

import io.helidon.common.features.api.Feature;
import io.helidon.common.features.api.HelidonFlavor;

/**
 * Helidon SE Config module.
 *
 * @see io.helidon.config
 */
@Feature(value = "Config",
        description = "Configuration module",
        in = HelidonFlavor.SE
)
module io.helidon.config {

    requires io.helidon.inject.api;

    requires static io.helidon.common.features.api;

    requires static jakarta.inject;
    requires static jakarta.annotation;

    requires transitive io.helidon.common.config;
    requires transitive io.helidon.common.media.type;
    requires transitive io.helidon.common;

    exports io.helidon.config;
    exports io.helidon.config.spi;

    uses io.helidon.config.spi.ConfigMapperProvider;
    uses io.helidon.config.spi.ConfigParser;
    uses io.helidon.config.spi.ConfigFilter;
    uses io.helidon.config.spi.ConfigSourceProvider;
    uses io.helidon.config.spi.OverrideSourceProvider;
    uses io.helidon.config.spi.RetryPolicyProvider;
    uses io.helidon.config.spi.PollingStrategyProvider;
    uses io.helidon.config.spi.ChangeWatcherProvider;

    provides io.helidon.config.spi.ConfigParser
            with io.helidon.config.PropertiesConfigParser;
    provides io.helidon.common.config.spi.ConfigProvider
            with io.helidon.config.HelidonConfigProvider;
    provides io.helidon.inject.api.ModuleComponent
            with io.helidon.config.Injection$$Module;

    // needed when running with modules - to make private methods accessible
    opens io.helidon.config to weld.core.impl, io.helidon.microprofile.cdi;

}
