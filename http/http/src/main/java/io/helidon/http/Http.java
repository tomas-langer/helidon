/*
 * Copyright (c) 2018, 2023 Oracle and/or its affiliates.
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

package io.helidon.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.helidon.service.inject.api.Injection;

/**
 * HTTP endpoint annotations.
 * <p>
 * In previous versions of Helidon, this class contained the following types:
 * <ul>
 * <li>{@link io.helidon.http.Method}</li>
 * <li>{@link io.helidon.http.Status}</li>
 * <li>{@link io.helidon.http.HeaderName}</li>
 * <li>{@link io.helidon.http.HeaderNames}</li>
 * <li>{@link io.helidon.http.Header}</li>
 * <li>{@link io.helidon.http.HeaderWriteable}</li>
 * <li>{@link io.helidon.http.HeaderValues}</li>
 * <li>{@link io.helidon.http.DateTime}</li>
 * </ul>
 */
public final class Http {
    private Http() {
    }

    /**
     * Path of an endpoint, or sub-path of a method.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.CLASS)
    @Documented
    public @interface Path {
        /**
         * Path to use, defaults to {@code /}.
         *
         * @return path to use
         */
        String value() default "/";
    }

    /**
     * Listener socket assigned to this endpoint.
     * This only makes sense for server side, as it is binding endpoint to a server socket.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.CLASS)
    @Documented
    public @interface Listener {
        /**
         * Name of a routing to bind this application/service to.
         *
         * @return name of a routing (or listener host/port) on WebServer
         */
        String value();

        /**
         * Set to true if the {@link #value()} MUST be configured.
         * <p>
         * The endpoint is bound to default listener if the {@link #value()} listener is not configured
         * on webserver, and this is set to {@code false}.
         *
         * @return {@code true} to enforce existence of the named routing
         */
        boolean required() default false;
    }

    /**
     * HTTP Method. Can be used as a meta annotation.
     */
    @Retention(RetentionPolicy.CLASS)
    @Documented
    @Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
    public @interface HttpMethod {
        /**
         * Text of the HTTP method.
         *
         * @return method
         */
        String value();
    }

    /**
     * Inject entity into a method parameter.
     */
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.CLASS)
    @Documented
    @Injection.Qualifier
    public @interface Entity {
    }

    /**
     * Inject header into a method parameter.
     */
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.CLASS)
    @Documented
    @Injection.Qualifier
    public @interface HeaderParam {
        /**
         * Name of the header.
         *
         * @return name of the header
         */
        String value();
    }

    /**
     * Inject path parameter into a method parameter.
     * Path parameters are obtained from the path template of the routing method.
     */
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.CLASS)
    @Documented
    @Injection.Qualifier
    public @interface PathParam {
        /**
         * Name of the parameter.
         *
         * @return name of the path parameter
         */
        String value();
    }

    /**
     * Status that should be returned. Only use when not setting it explicitly.
     * If an exception is thrown from the method, status is determined based on
     * error handling.
     * <p>
     * You can use {@code _INT} constants from {@link io.helidon.http.Status} for
     * {@link #value()}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface Status {
        /**
         * Status code to use.
         *
         * @return status code
         */
        int value();

        /**
         * If this is a non-standard status, add a custom reason to it.
         *
         * @return reason to use
         */
        String reason() default "";
    }

    /**
     * GET method of an HTTP endpoint.
     */
    @Retention(RetentionPolicy.CLASS)
    @Documented
    @HttpMethod(Method.GET_STRING)
    public @interface GET {
    }

    /**
     * POST method of an HTTP endpoint.
     */
    @Retention(RetentionPolicy.CLASS)
    @Documented
    @HttpMethod(Method.POST_STRING)
    public @interface POST {
    }

    /**
     * PUT method of an HTTP endpoint.
     */
    @Retention(RetentionPolicy.CLASS)
    @Documented
    @HttpMethod(Method.PUT_STRING)
    public @interface PUT {
    }

    /**
     * DELETE method of an HTTP endpoint.
     */
    @Retention(RetentionPolicy.CLASS)
    @Documented
    @HttpMethod(Method.DELETE_STRING)
    public @interface DELETE {
    }

    /**
     * HEAD method of an HTTP endpoint.
     */
    @Retention(RetentionPolicy.CLASS)
    @Documented
    @HttpMethod(Method.HEAD_STRING)
    public @interface HEAD {
    }

    /**
     * PATCH method of an HTTP endpoint.
     */
    @Retention(RetentionPolicy.CLASS)
    @Documented
    @HttpMethod(Method.PATCH_STRING)
    public @interface PATCH {
    }

    /**
     * PATCH method of an HTTP endpoint.
     */
    @Retention(RetentionPolicy.CLASS)
    @Documented
    public @interface Produces {
        /**
         * Media types that may be returned by this endpoint.
         *
         * @return
         */
        String[] value();
    }

    /**
     * PATCH method of an HTTP endpoint.
     */
    @Retention(RetentionPolicy.CLASS)
    @Documented
    public @interface Consumes {
    }
}
