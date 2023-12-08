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

package io.helidon.common.types;

import java.util.List;
import java.util.Map;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Module info.
 */
@Prototype.Blueprint
interface ModuleInfoBlueprint {
    /**
     * Name of the module.
     *
     * @return module name
     */
    String name();

    /**
     * Whether this module is declared as open module.
     *
     * @return whether this module is open
     */
    @Option.DefaultBoolean(false)
    boolean isOpen();

    /**
     * Declared dependencies of the module.
     *
     * @return list of requires
     */
    @Option.Singular
    List<ModuleInfoRequires> requires();

    /**
     * Exports of the module.
     *
     * @return list of exported packages
     */
    @Option.Singular
    List<String> exports();

    /**
     * Used service loader providers.
     *
     * @return list of used provider interfaces
     */
    @Option.Singular
    List<TypeName> uses();

    /**
     * Map of provider interfaces to provider implementations provided by this module.
     *
     * @return map of interface to implementations
     */
    @Option.Singular
    Map<TypeName, List<TypeName>> provides();

    /**
     * Map of opened packages to modules (if any).
     *
     * @return map of package to modules
     */
    @Option.Singular
    Map<String, List<String>> opens();

}
