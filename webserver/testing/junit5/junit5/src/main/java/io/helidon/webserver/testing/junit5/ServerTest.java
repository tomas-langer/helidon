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

package io.helidon.webserver.testing.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test of server that opens a socket (for integration tests).
 * Can be used together with:
 * <ul>
 *     <li>{@link SetUpRoute}</li>
 *     <li>{@link SetUpServer}</li>
 * </ul>
 * <p>
 * When {@link #useRegistry()} is set to {@code true}, only services discovered through service registry
 * would be used
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(HelidonServerJunitExtension.class)
@Inherited
public @interface ServerTest {
    /**
     * Whether to use inject registry to start the server.
     * When using registry, the routing is constructed via inversion of control.
     *
     * @return whether to use service registry, defaults to {@code true} if inject service registry is on classpath
     */
    boolean useRegistry() default true;
}
