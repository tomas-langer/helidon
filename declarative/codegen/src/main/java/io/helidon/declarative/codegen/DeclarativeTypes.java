/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

package io.helidon.declarative.codegen;

import java.util.Set;

import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeName;

import static io.helidon.service.codegen.ServiceCodegenTypes.INJECTION_SINGLETON;

public final class DeclarativeTypes {
    public static final TypeName THROWABLE = TypeName.create(Throwable.class);
    public static final Annotation SINGLETON_ANNOTATION = Annotation.create(INJECTION_SINGLETON);
    public static final TypeName SET_OF_THROWABLES = TypeName.builder(TypeName.create(Set.class))
            .addTypeArgument(TypeName.builder(TypeName.create(Class.class))
                                     .addTypeArgument(TypeName.builder(TypeName.create(Throwable.class))
                                                              .wildcard(true)
                                                              .build())
                                     .build())
            .build();

    private DeclarativeTypes() {
    }
}
