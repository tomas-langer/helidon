/*
 * Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.microprofile.cdi;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;

/**
 * Build time of the application (as opposed to runtime).
 * See {@link RuntimeScoped} for detailed explanation.
 *
 * <p>Example of usage in a CDI {@link javax.enterprise.inject.spi.Extension}:
 * <pre>
 * void initBuildTime(@Observes @Initialized(@BuildTimeScoped.class) Object event) {}
 * void endBuildTime(@Observes @BeforeDestroyed(@BuildTimeScoped.class) Object event) {}
 * </pre>
 * The initialization happens as soon as possible within CDI bootstrap.
 * The before destroyed (and destroyed) are called when initialization is done. In case of native-image, this would be before
 * the native image generation starts.
 *
 * <p>Even though the observer method behaves similar to a CDI Scope, and the class is
 * named "Scoped", this is not a Scope annotation (yet).
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface BuildTimeScoped {
    /**
     * A literal for initialized build time scope.
     */
    Initialized INITIALIZED_LITERAL = Initialized.Literal.of(BuildTimeScoped.class);
    /**
     * A literal for before destroyed build time scope.
     */
    BeforeDestroyed BEFORE_DESTROYED_LITERAL = BeforeDestroyed.Literal.of(BuildTimeScoped.class);
    /**
     * A literal for destroyed build time scope.
     */
    Destroyed DESTROYED_LITERAL = Destroyed.Literal.of(BuildTimeScoped.class);
}
