package io.helidon.di.annotation.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.helidon.common.http.Http;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
@Target({ElementType.PARAMETER})
public @interface Client {
    /**
     * This is the endpoint (protocol, host, port) or service id (when using service discovery.
     *
     * @return endpoint
     */
    String value() default "";

    /**
     * A named client - as found in configuration.
     *
     * @return name of the client
     */
    String name() default "";

    /**
     * Base path of the client.
     *
     * @return base path
     */
    String basePath() default "";

    /**
     * HTTP version to use.
     *
     * @return HTTP version
     */
    Http.Version version() default Http.Version.V1_1;

}
