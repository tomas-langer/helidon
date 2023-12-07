/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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

package io.helidon.inject.service;

import io.helidon.builder.api.Prototype;
import io.helidon.common.types.Annotation;

/**
 * Represents a qualifier annotation (a specific case of annotations, annotated with
 * {@link io.helidon.inject.service.Inject.Qualifier}.
 *
 * @see io.helidon.inject.service.Inject.Qualifier
 */
@Prototype.Blueprint
@Prototype.CustomMethods(QualifierSupport.CustomMethods.class)
interface QualifierBlueprint extends Annotation {
    /**
     * Represents a wildcard {@link io.helidon.inject.service.Inject.Named} qualifier.
     */
    Qualifier WILDCARD_NAMED = Qualifier.createNamed(Inject.Named.WILDCARD_NAME);
    /**
     * Represents an instance named with the default name: {@value io.helidon.inject.service.Inject.Named#DEFAULT_NAME}.
     */
    Qualifier DEFAULT_NAMED = Qualifier.createNamed(Inject.Named.DEFAULT_NAME);
}
