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
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.helidon.webserver.Service;

/**
 * Add a {@link io.helidon.webserver.Service} to server.
 * This annotation can be repeated.
 * If you annotate a method, the method signatures must be
 * either {@code static Service anyMethodName()} or {@code static Service anyMethodName(Config)}. The config provided
 * is located at the root of configuration tree. The Service class must be the same as the one in {@link #value()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(AddServices.class)
public @interface AddService {
    String UNCONFIGURED_PATH = "\\tsfd\\unconfigured_value\\tsfd\\";

    /**
     * Class of the service to add. The class must have no-arg constructor.
     * @return service class
     */
    Class<? extends Service> value();

    /**
     * Path pattern to register the service on
     * @return path
     */
    String path() default UNCONFIGURED_PATH;

    /**
     * If set to {@code true} we look for a {@code static X create(Config)} method to create an instance.
     * This is ignored if the service is created using a method.
     *
     * @return whether this service uses configuration
     */
    boolean configured() default false;

    /**
     * Configuration key from the root of the config to provide to create method of this service
     * if {@link #configured()}.
     * This is ignored if the service is created using a method.
     *
     * @return configuration key, such as "metrics"
     */
    String configKey() default "";
}
