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
 * API requires for code generating classes required for Helidon Inject.
 * This is the minimal set of types that must be available.
 */
/*
Note for developers:
This modules MUST be a compile & runtime dependency of any module that exposes services,
as the ModuleComponent is a ServiceLoader provider interface. As such, it must be set as
provides ModuleComponent with Generated__ModuleComponent
in the module-info.java of the project. If a module provides a service, the module of the service must not be a static
dependency
 */
module io.helidon.inject.service {
    // required for generated code
    requires transitive io.helidon.common.types;
    requires io.helidon.builder.api;

    exports io.helidon.inject.service;
}