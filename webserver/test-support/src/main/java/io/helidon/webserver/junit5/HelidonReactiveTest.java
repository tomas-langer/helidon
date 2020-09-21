/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.webserver.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This test will start a Helidon WebServer before the first test method is executed and shut it down
 * after the last one.
 * The server runs (by default) on a random port.
 * <p>
 * The following constructor or method parameters are supported:
 * <ul>
 *     <li>{@link io.helidon.webclient.WebClient} - an instance of web client pointing to the host and port of the web server
 *     .</li>
 *     <li>{@link io.helidon.config.Config} - configuration of the current test, honoring all
 *     {@link io.helidon.webserver.junit5.AddConfig} annotations</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtendWith(HelidonSeJunitExtension.class)
public @interface HelidonReactiveTest {
}
