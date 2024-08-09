package io.helidon.webserver.testing.junit5;

import java.lang.reflect.Method;

import io.helidon.common.LazyValue;
import io.helidon.service.inject.api.InjectRegistry;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.ServiceRegistry;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.WebServerService__ServiceDescriptor;

/**
 * This class should only be called when service registry is enabled
 */
class ServiceRegistrySupport {
    private static final LazyValue<Boolean> enabled = LazyValue.create(() -> {
        try {
            Class.forName("io.helidon.service.inject.ApplicationMain");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    });

    public static Object param(Class<?> paramType) {
        return GlobalServiceRegistry.registry();
    }

    static boolean supportedType(Class<?> paramType) {
        return InjectRegistry.class.equals(paramType) || ServiceRegistry.class.equals(paramType);
    }

    static boolean enabled() {
        return enabled.get();
    }

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
