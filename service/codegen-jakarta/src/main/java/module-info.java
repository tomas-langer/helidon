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

/**
 * Code generation extension to support Jakarta inject (JSR-330) in {@code jakarta} packages.
 */
module io.helidon.service.codegen.jakarta {
    requires io.helidon.service.codegen;

    exports io.helidon.service.codegen.jakarta;

    provides io.helidon.service.codegen.spi.InjectAssignmentProvider
            with io.helidon.service.codegen.jakarta.JakartaAssignmentProvider;

    provides io.helidon.service.codegen.spi.RegistryCodegenExtensionProvider
            with io.helidon.service.codegen.jakarta.JakartaExtensionProvider,
                    io.helidon.service.codegen.jakarta.UnsupportedTypesExtensionProvider;

    provides io.helidon.codegen.spi.AnnotationMapperProvider
            with io.helidon.service.codegen.jakarta.MapJakartaProvider,
                    io.helidon.service.codegen.jakarta.MapApplicationScopedProvider;
}