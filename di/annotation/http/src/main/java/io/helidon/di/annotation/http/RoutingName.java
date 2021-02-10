package io.helidon.di.annotation.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a {@code io.helidon.webserver.Service} to a specific (named) routing
 *  on {@code io.helidon.webserver.WebServer}. The routing should have a corresponding named socket configured on the
 *  WebServer to run the routing on.
 *
 * Configuration can be overridden using configuration:
 * <ul>
 *     <li>Name of routing: {@code fully.qualified.ClassName.routing.name} to change the name of the named routing.
 *      Use {@value #DEFAULT_NAME} to revert to default named routing.
 *      </li>
 *     <li>Whether routing is required: {@code fully.qualified.ClassName.routing.name-required}</li>
 * </ul>
 *
 * Example class:
 * <pre>
 * {@literal @}Routing
 * {@literal @}RoutingPath("/myservice")
 * {@literal @}RoutingName(value = "admin", required = true)
 * public class MyService implements Service {
 *     {@literal @}Override
 *     public void update(Routing.Rules rules) {
 *         {@code rules.get("/hello", (req, res) -> res.send("Hello WebServer"));}
 *     }
 * }
 * </pre>
 * Example configuration (yaml):
 * <pre>
 * com.example.MyService.routing:
 *  name: "@default"
 *  name-required: false
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface RoutingName {
    /**
     * Configuration key of the routing path, appended after the fully qualified class name (does not contain the leading dot).
     */
    String CONFIG_KEY_NAME = "routing.name";
    /**
     * Configuration key of the routing name required flag,
     * appended after the fully qualified class name (does not contain the leading dot).
     */
    String CONFIG_KEY_REQUIRED = "routing.name-required";

    /**
     * Name of a routing to bind this application/service to.
     * @return name of a routing (or listener host/port) on WebServer
     */
    String value();

    /**
     * Set to true if the {@link #value()} MUST be configured.
     *
     * @return {@code true} to enforce existence of the named routing
     */
    boolean required() default false;
}
