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

package io.helidon.inject.tests.configbeans;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

@Prototype.Blueprint
@Prototype.Configured
interface EnumRelatedConfigBlueprint {

    FakeClientAuth clientAuth();

    Optional<FakeClientAuth> optionalClientAuth();

    @Option.Singular
    List<InlinedEnum> list();

    @Option.Singular
    Set<InlinedEnum> set();

    @Option.Singular
    Map<String, InlinedEnum> map();

    enum InlinedEnum {
        TEST
    }

}
