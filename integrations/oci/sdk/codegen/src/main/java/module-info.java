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

import io.helidon.integrations.oci.sdk.codegen.OciInjectCodegenObserverProvider;

/**
 * Helidon Injection Integrations for OCI SDK.
 */
module io.helidon.integrations.oci.sdk.codegen {
    requires io.helidon.codegen;
    requires io.helidon.codegen.classmodel;

    requires transitive io.helidon.common.types;
    requires transitive io.helidon.inject.codegen;

    exports io.helidon.integrations.oci.sdk.codegen;

    provides io.helidon.inject.codegen.spi.InjectCodegenObserverProvider
            with OciInjectCodegenObserverProvider;

}
