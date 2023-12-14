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

package io.helidon.webserver;

import io.helidon.common.config.GlobalConfig;
import io.helidon.config.Config;
import io.helidon.inject.InjectionConfig;
import io.helidon.inject.InjectionServices;
import io.helidon.inject.Lookup;
import io.helidon.inject.Phase;
import io.helidon.inject.ServiceProvider;
import io.helidon.inject.Services;
import io.helidon.inject.testing.InjectionTestingSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@Disabled
class WebServerConfigDrivenTest {
    static final boolean NORMAL_PRODUCTION_PATH = false;

    @AfterEach
    public void reset() {
        if (!NORMAL_PRODUCTION_PATH) {
            // requires 'inject.permits-dynamic=true' to be able to reset
            InjectionTestingSupport.resetAll();
        }
    }

    @Test
    void testConfigDriven() {
        // This will pick up application.yaml from the classpath as default configuration file
        Config config = Config.create();
        GlobalConfig.config(() -> config, true);
        InjectionConfig cfg = InjectionConfig.create(config.get("inject"));

        if (NORMAL_PRODUCTION_PATH) {
            // bootstrap Injection with our config tree when it initializes
            InjectionServices.configure(cfg);
        }

        // initialize Injection, and drive all activations based upon what has been configured
        Services services;
        if (NORMAL_PRODUCTION_PATH) {
            services = InjectionServices.instance().services();
        } else {
            InjectionServices injectionServices = InjectionTestingSupport.testableServices(cfg);
            services = injectionServices.services();
        }

        ServiceProvider<WebServer> webServerSp = services.firstServiceProvider(Lookup.create(WebServer.class));
        assertThat(webServerSp.currentActivationPhase(), is(Phase.ACTIVE));
    }

}
