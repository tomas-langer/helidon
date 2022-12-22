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

package io.helidon.pico.testing;

import io.helidon.pico.PicoServicesHolder;

/**
 * Supporting helper utilities unit-testing Pico services.
 */
public class PicoTestingSupport {

    /**
     * Resets all internal Pico configuration instances, JVM global singletons, service registries, etc.
     */
    public static void resetAll() {
        Holder.reset();
    }


    private static class Holder extends PicoServicesHolder {
        public static void reset() {
            PicoServicesHolder.reset();
        }
    }

}
