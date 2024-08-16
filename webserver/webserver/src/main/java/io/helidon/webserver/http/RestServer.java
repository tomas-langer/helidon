package io.helidon.webserver.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Optional;

/**
 * APIs to define a declarative server endpoint.
 */
public final class RestServer {
    private RestServer() {
    }

    /**
     * Definition of a server endpoint.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.CLASS)
    @Documented
    @Inherited
    public @interface Endpoint {
    }

    /**
     * Definition of an outbound header (sent with every response).
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Repeatable(Headers.class)
    @Documented
    public @interface Header {
        /**
         * Name of the header, see {@link io.helidon.http.HeaderNames} constants with {@code _STRING}.
         *
         * @return header name
         */
        String name();

        /**
         * Value of the header.
         *
         * @return header value
         */
        String value();
    }

    /**
     * Container for {@link io.helidon.webserver.http.RestServer.Header} repeated annotation.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Documented
    public @interface Headers {
        /**
         * Headers to add o request.
         *
         * @return headers
         */
        Header[] value();
    }

    /**
     * Definition of an outbound header (sent with every request).
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Documented
    @Repeatable(ComputedHeaders.class)
    public @interface ComputedHeader {
        /**
         * Name of the header, see {@link io.helidon.http.HeaderNames} constants with {@code _STRING}.
         *
         * @return header name
         */
        String name();

        /**
         * A producer type, must be a {@link io.helidon.service.registry.ServiceRegistry} service.
         * <p>
         * The producer may not produce a value, see {@link #required()}.
         *
         * @return producer to get header value from
         */
        Class<? extends HeaderProducer> producerClass();

        /**
         * If the {@link #producerClass()} produces an empty optional, and required is set to {@code true}
         * (default), we will throw an exception. Otherwise, the header will not be configured on the request.
         *
         * @return whether the header must be provided
         */
        boolean required() default true;
    }

    /**
     * Container for {@link io.helidon.webserver.http.RestServer.ComputedHeader} repeated annotation.
     */
    @Target({ElementType.TYPE, ElementType.METHOD})
    @Documented
    public @interface ComputedHeaders {
        /**
         * Headers to add o request.
         *
         * @return headers
         */
        ComputedHeader[] value();
    }

    /**
     * Header producer, to use with {@link io.helidon.webserver.http.RestServer.ComputedHeader#producerClass()}.
     */
    public interface HeaderProducer {
        /**
         * Produce an instance of a named header.
         *
         * @param name name to create
         * @return value for the header
         */
        Optional<String> produceHeader(String name);
    }

    /**
     * Listener socket assigned to this endpoint.
     * This only makes sense for server side, as it is binding endpoint to a server socket.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.CLASS)
    @Documented
    @Inherited
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
     * Status that should be returned. Only use when not setting it explicitly.
     * If an exception is thrown from the method, status is determined based on
     * error handling.
     * <p>
     * You can use {@code _INT} constants from {@link io.helidon.http.Status} for
     * {@link #value()}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Inherited
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
}
