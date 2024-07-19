package io.helidon.webserver.testing.junit5;

import java.lang.reflect.Method;

import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.WebServerService__ServiceDescriptor;

/**
 * This class should only be called when service registry is enabled
 */
class ServiceRegistrySupport {
    static void setup(WebServerConfig.Builder serverBuilder) {
        Object o = GlobalServiceRegistry.registry()
                .get(WebServerService__ServiceDescriptor.INSTANCE)
                .orElseThrow(() -> {
                    return new IllegalStateException("Could not discover WebServer in service registry, both "
                                                             + "'helidon-service-inject' and `helidon-webserver` must be on "
                                                             + "classpath.");
                });
        // the service is package local
        Class<?> clazz = o.getClass();
        try {
            Method method = clazz.getDeclaredMethod("updateServerBuilder", WebServerConfig.BuilderBase.class);
            method.setAccessible(true);
            method.invoke(o, serverBuilder);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to get service registry specific method on WebServerService", e);
        }
    }
}
