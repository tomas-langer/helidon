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

package io.helidon.examples.inject.configdriven;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.examples.inject.basics.ToolBox;
import io.helidon.inject.Bootstrap;
import io.helidon.inject.InjectionServices;
import io.helidon.inject.Services;

/**
 * Config-driven example.
 */
public class Main {

    /**
     * Executes the example.
     *
     * @param args arguments
     */
    public static void main(String... args) {
        // we need to first initialize Injection - informing the framework where to find the application's Config
        Config config = Config.builder()
                .addSource(ConfigSources.classpath("application.yaml"))
                .disableSystemPropertiesSource()
                .disableEnvironmentVariablesSource()
                .build();
        Bootstrap bootstrap = Bootstrap.builder()
                .config(config)
                .build();
        InjectionServices.globalBootstrap(bootstrap);

        // this drives config-driven service activations (see the contents of the toolbox being output)
        Services services = InjectionServices.realizedServices();

        // this will trigger the PostConstruct method to display the contents of the toolbox
        services.lookupFirst(ToolBox.class).get();
    }

}
