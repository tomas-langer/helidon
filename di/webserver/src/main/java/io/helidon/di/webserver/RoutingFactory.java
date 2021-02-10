package io.helidon.di.webserver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import javax.inject.Singleton;

import io.helidon.config.Config;
import io.helidon.di.annotation.http.Path;
import io.helidon.di.annotation.http.RoutingName;
import io.helidon.webserver.Routing;
import io.helidon.webserver.Service;
import io.helidon.webserver.WebServer;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Factory;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.order.OrderUtil;
import io.micronaut.inject.BeanDefinition;

@Factory
class RoutingFactory {
    private static final Logger LOGGER = java.util.logging.Logger.getLogger(RoutingFactory.class.getName());

    private final RouteBuilders routeBuilders;
    private final Config config;

    protected RoutingFactory(RouteBuilders routeBuilders, Config config) {
        this.routeBuilders = routeBuilders;
        this.config = config;
    }

    @Singleton
    protected Routes routing(BeanContext context, List<Service> services) {
        OrderUtil.sort(services);
        for (Service service : services) {
            BeanDefinition<?> beanDefinition = context.getBeanDefinition(service.getClass());
            String serviceClassName = beanDefinition.getBeanType().getName();
            AnnotationMetadata meta = beanDefinition.getAnnotationMetadata();
            Optional<String> routingPath = routingPath(config, serviceClassName, meta);
            Routing.Builder routing = routingBuilder(config, routeBuilders, serviceClassName, meta);

            if (routingPath.isPresent()) {
                String path = routingPath.get();
                LOGGER.finest(() -> "Registering helidon service " + service.getClass().getName() + " on path " + path);
                routing.register(path, service);
            } else {
                LOGGER.finest(() -> "Registering helidon service " + service.getClass().getName());
                routing.register(service);
            }
        }

        Map<String, Routing> routings = new HashMap<>();
        Map<String, Routing.Builder> routes = routeBuilders.routes();

        routes.forEach((key, value) -> routings.put(key, value.build()));

        return new Routes(routings);
    }

    static Optional<String> routingPath(Config config, String serviceClassName, AnnotationMetadata meta) {
        return config.get(serviceClassName + "." + Path.CONFIG_KEY_PATH)
                .asString()
                .or(() -> meta.stringValue(Path.class));
    }

    static Routing.Builder routingBuilder(Config config, RouteBuilders routeBuilders,
                                          String serviceClassName, AnnotationMetadata meta) {
        // if not configured, use values from annotation
        Optional<AnnotationValue<RoutingName>> annotation = meta.findAnnotation(RoutingName.class);

        // first see if we have configuration override of the routing name
        String routingName = config.get(serviceClassName + "." + RoutingName.CONFIG_KEY_NAME)
                .asString()
                .or(() -> annotation.flatMap(AnnotationValue::stringValue))
                .orElse(WebServer.DEFAULT_SOCKET_NAME);

        boolean required = config.get(serviceClassName + "." + RoutingName.CONFIG_KEY_REQUIRED)
                .asBoolean()
                .or(() -> annotation.flatMap(it -> it.booleanValue("required")))
                .orElse(false);


        return routeBuilders.routing(routingName)
                .orElseGet(() -> {
                    if (required) {
                        throw new IllegalStateException("Service " + serviceClassName + " requires routing named " + routingName
                                                                + " yet no such socket is configured on web server.");
                    }
                    return routeBuilders.routing();
                });
    }

}
