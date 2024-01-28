/*
 * Copyright (c) 2023, 2024 Oracle and/or its affiliates.
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

import io.helidon.service.codegen.spi.InjectAssignmentProvider;
import io.helidon.service.codegen.spi.InjectCodegenExtensionProvider;
import io.helidon.service.codegen.spi.InjectCodegenObserverProvider;

/**
 * Code generation for Helidon Inject.
 */
module io.helidon.service.codegen {
    requires transitive io.helidon.builder.api;
    requires transitive io.helidon.codegen.classmodel;
    requires transitive io.helidon.codegen;

    exports io.helidon.service.codegen;
    exports io.helidon.service.codegen.spi;

    uses InjectCodegenObserverProvider;
    uses InjectCodegenExtensionProvider;
    uses InjectAssignmentProvider;

    provides io.helidon.codegen.spi.CodegenExtensionProvider
            with io.helidon.service.codegen.InjectCodegenProvider;

    provides InjectCodegenExtensionProvider
            with io.helidon.service.codegen.InjectionExtensionProvider,
                    io.helidon.service.codegen.ConfigBeanCodegenProvider;

    provides io.helidon.codegen.spi.AnnotationMapperProvider
            with io.helidon.service.codegen.MapClassNamedProvider,
                    io.helidon.service.codegen.MapDrivenBy;
}