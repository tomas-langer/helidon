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
 * Run time of the application (as opposed to build time).
 * <p>This is from the point of view of ahead of time compilation, such as when using GraalVM native-image.
 * <p>There are two phases of an application lifecycle:
 * <ul>
 *     <li>{@link BuildTimeScoped} - Application should not connect anywhere, this is to initialize
 *      the environment before {@code native-image} is generated. Configuration available is build-time specific and
 *      MUST not be used for runtime operation of this application</li>
 *      <li>Runtime (this annotation) - Application is starting and should set up all resources. Configuration
 *      available at runtime is intended for runtime of this application</li>
 * </ul>
 *
 * <p>Example of usage in a CDI {@link javax.enterprise.inject.spi.Extension}:
 * <pre>
 * void initRuntime(@Observes @Initialized(@RunTimeScoped.class) Object event) {}
 * void endRuntime(@Observes @BeforeDestroyed(@RunTimeScoped.class) Object event) {}
 * </pre>
 * <p>Although we support both {@link javax.enterprise.context.Initialized} and
 *  {@link javax.enterprise.context.BeforeDestroyed} qualifiers, the {@link javax.enterprise.context.BeforeDestroyed}
 *  is equivalent to the {@link javax.enterprise.context.ApplicationScoped} being destroyed.
 * <p>{@link javax.enterprise.context.Initialized} is triggered as soon as possible after the call of
 * {@link HelidonContainer#start()} - even before the {@link javax.enterprise.context.ApplicationScoped} is initialized.
 *
 * <p>Even though the observer method behave similar to a CDI Scope and the class is named
 * accordingly, this is not a Scope annotation (yet).
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface RuntimeScoped {
    /**
     * A literal for initialized runtime scope.
     */
    Initialized INITIALIZED_LITERAL = Initialized.Literal.of(RuntimeScoped.class);
    /**
     * A literal for before destroyed runtime scope.
     */
    BeforeDestroyed BEFORE_DESTROYED_LITERAL = BeforeDestroyed.Literal.of(RuntimeScoped.class);
    /**
     * A literal for destroyed runtime scope.
     */
    Destroyed DESTROYED_LITERAL = Destroyed.Literal.of(RuntimeScoped.class);
}
