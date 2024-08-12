/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

package io.helidon.webserver.testing.junit5;

import java.lang.reflect.Method;

import io.helidon.common.LazyValue;
import io.helidon.service.inject.api.InjectRegistry;
import io.helidon.service.registry.GlobalServiceRegistry;
import io.helidon.service.registry.ServiceRegistry;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.WebServerService__ServiceDescriptor;

/**
 * Check if injection is enabled. If so, also provides support for testing with injection registry.
 */
final class InjectionSupport {
    private static final LazyValue<Boolean> ENABLED = LazyValue.create(() -> {
        try {
            Class.forName("io.helidon.service.inject.ApplicationMain");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    });

    private InjectionSupport() {
    }

    static Object param(Class<?> paramType) {
        return GlobalServiceRegistry.registry()
                .get(paramType);
    }

    static boolean supportedType(Class<?> paramType) {
        if (InjectRegistry.class.equals(paramType) || ServiceRegistry.class.equals(paramType)) {
            return true;
        }
        // we do not want to get the instance here (yet)
        return !GlobalServiceRegistry.registry()
                .allServices(paramType)
                .isEmpty();
    }

    static boolean enabled() {
        return ENABLED.get();
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
