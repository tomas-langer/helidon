package io.helidon.di.annotation.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Singleton;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.DefaultScope;

/**
 * Path of a {@code io.helidon.webserver.Service} or {@link io.helidon.di.annotation.http.Controller}
 * to register with routing.
 * If a service is not annotated with this annotation, it would be registered without a path using
 * {@code io.helidon.webserver.Routing.Rules.register(io.helidon.webserver.Service...)}.
 * If a {@link io.helidon.di.annotation.http.Controller} is not annotation, it would be register at
 * {@code /}.
 *
 * Configuration can be overridden using configuration:
 * <ul>
 *     <li>{@code fully.qualified.ClassName.routing-path.path} to change the path.</li>
 * </ul>
 *
 * Example class:
 * <pre>
 * {@literal @}Routing()
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
 *  path: "/myservice-customized"
 * </pre>
 * Default scope is Singleton.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Bean
@DefaultScope(Singleton.class)
public @interface Path {
    /**
     * Configuration key of the routing path, appended after the fully qualified class name (does not contain the leading dot).
     */
    String CONFIG_KEY_PATH = "routing.path";

    /**
     * Path of this WebServer service. Use the same path as would be used with {@link io.helidon.webserver.Routing.Rules}.
     * @return path to register the service on.
     */
    String value();
}
