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

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.helidon.common.LogConfig;
import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.media.common.MediaContext;
import io.helidon.media.common.MediaSupport;
import io.helidon.webclient.WebClient;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.testsupport.TestClient;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Junit5 extension to support Helidon CDI container in tests.
 */
class HelidonSeJunitExtension implements BeforeAllCallback,
                                         AfterAllCallback,
                                         InvocationInterceptor,
                                         BeforeEachCallback,
                                         AfterEachCallback,
                                         ParameterResolver {
    private static final Set<Class<?>> SUPPORTED_PARAM_TYPES = new HashSet<>();

    static {
        SUPPORTED_PARAM_TYPES.add(WebClient.class);
        SUPPORTED_PARAM_TYPES.add(Config.class);
        SUPPORTED_PARAM_TYPES.add(TestClient.class);
        SUPPORTED_PARAM_TYPES.add(WebServer.class);
    }

    private final List<AddService> classLevelServices = new ArrayList<>();
    private final List<AddMedia> classLevelMedia = new ArrayList<>();
    private final ConfigMeta classLevelConfigMeta = new ConfigMeta();

    private Class<?> testClass;
    private Config config;
    private Config methodConfig;
    private WebServer webServer;
    private WebClient webClient;
    private boolean local;
    private Routing routing;
    private MediaContext mediaContext;
    private TestClient testClient;

    @Override
    public void beforeAll(ExtensionContext context) {
        LogConfig.initClass();
        testClass = context.getRequiredTestClass();

        HelidonReactiveTest test = testClass.getAnnotation(HelidonReactiveTest.class);
        local = test.local();

        AddConfig[] configs = testClass.getAnnotationsByType(AddConfig.class);
        classLevelConfigMeta.addConfig(configs);
        classLevelConfigMeta.configuration(testClass.getAnnotation(Configuration.class));

        AddService[] services = testClass.getAnnotationsByType(AddService.class);
        classLevelServices.addAll(Arrays.asList(services));

        AddMedia[] media = testClass.getAnnotationsByType(AddMedia.class);
        classLevelMedia.addAll(Arrays.asList(media));

        config = configure(classLevelConfigMeta);

        routing = createRouting(classLevelServices);
        mediaContext = createMediaContext(classLevelMedia);

        testClient = TestClient.create(routing, mediaContext);

        if (!local) {
            webServer = WebServer.builder()
                    .routing(routing)
                    .mediaContext(mediaContext)
                    .build()
                    .start()
                    .await(10, TimeUnit.SECONDS);

            webClient = WebClient.builder()
                    .config(config.get("webclient"))
                    .mediaContext(mediaContext)
                    .baseUri("http://localhost:" + webServer.port())
                    .build();
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        Method method = context.getRequiredTestMethod();
        AddConfig[] configs = method.getAnnotationsByType(AddConfig.class);

        if (configs.length > 0) {
            ConfigMeta methodLevelConfigMeta = classLevelConfigMeta.nextMethod();
            methodLevelConfigMeta.addConfig(configs);
            methodLevelConfigMeta.configuration(method.getAnnotation(Configuration.class));

            methodConfig = configure(methodLevelConfigMeta);
        } else {
            methodConfig = config;
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        this.methodConfig = null;
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (webServer != null) {
            webServer.shutdown()
                    .await(10, TimeUnit.SECONDS);
            webServer = null;
        }
        webClient = null;
        testClient = null;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {

        Class<?> paramType = parameterContext.getParameter().getType();

        return SUPPORTED_PARAM_TYPES.contains(paramType);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {

        Class<?> parameterType = parameterContext.getParameter().getType();

        if (parameterType.equals(WebClient.class)) {
            if (local) {
                throw new IllegalStateException("Cannot resolve WebClient parameter when test is running locally");
            }
            return webClient;
        }

        if (parameterType.equals(WebServer.class)) {
            if (local) {
                throw new IllegalStateException("Cannot resolve WebServer parameter when test is running locally");
            }
            return webServer;
        }

        if (parameterType.equals(TestClient.class)) {
            return testClient;
        }

        if (parameterType.equals(Config.class)) {
            if (methodConfig == null) {
                return config;
            }
            return methodConfig;
        }

        return null;
    }

    private Config configure(ConfigMeta configMeta) {
        // only create a custom configuration if not provided by test method/class
        // prepare configuration
        Config.Builder builder = Config.builder();

        configMeta.additionalSources.forEach(it -> ConfigSources.classpathAll(it)
                .forEach(builder::addSource));

        return builder
                .addSource(ConfigSources.create(configMeta.additionalKeys))
                .addSource(ConfigSources.classpath("application.yaml").optional())
                .addSource(ConfigSources.classpath("application-test.yaml").optional())
                .build();
    }

    private MediaContext createMediaContext(List<AddMedia> media) {
        MediaContext.Builder builder = MediaContext.builder();

        for (AddMedia addMedia : media) {
            MediaSupport mediaSupport = instantiate(addMedia.value());
            ;
            builder.addMediaSupport(mediaSupport);
        }

        return builder.build();
    }

    private Routing createRouting(List<AddService> services) {
        Set<Method> routeMethods = new HashSet<>();
        Set<Method> serviceMethods = new HashSet<>();
        addRouteMethods(routeMethods, serviceMethods, testClass.getMethods());
        addRouteMethods(routeMethods, serviceMethods, testClass.getDeclaredMethods());

        Set<Method> setupRoutingMethods = new LinkedHashSet<>();
        addSetupRoutingMethods(setupRoutingMethods, testClass.getMethods());
        addSetupRoutingMethods(setupRoutingMethods, testClass.getDeclaredMethods());

        if (setupRoutingMethods.size() > 0) {
            if (setupRoutingMethods.size() > 1) {
                throw new IllegalStateException("There can only be one method annotated with @SetupRouting");
            }
            if (routeMethods.size() > 0) {
                throw new IllegalStateException(
                        "If there is a method annotated with @SetupRouting, you cannot add methods with @AddRoute annotation");
            }
            if (serviceMethods.size() > 0) {
                throw new IllegalStateException(
                        "If there is a method annotated with @SetupRouting, you cannot add methods with @AddService annotation");
            }
            Method m = setupRoutingMethods.iterator().next();
            return invokeMethodWithOptionalConfig(m);
        }

        Routing.Builder routing = Routing.builder();

        registerRouteMethods(routing, routeMethods);
        registerServices(routing, serviceMethods, services);

        return routing.build();
    }

    private void registerServices(Routing.Builder routing,
                                  Set<Method> serviceMethods,
                                  List<AddService> services) {
        for (AddService service : services) {
            Service serviceInstance;

            if (service.configured()) {
                Config serviceConfig = config;
                if (!service.configKey().isBlank()) {
                    serviceConfig = config.get(service.configKey());
                }
                serviceInstance = instantiate(service.value(), serviceConfig);
            } else {
                serviceInstance = instantiate(service.value());

            }
            if (service.path().equals(AddService.UNCONFIGURED_PATH)) {
                routing.register(serviceInstance);
            } else {
                routing.register(service.path(), serviceInstance);
            }
        }
        for (Method serviceMethod : serviceMethods) {
            AddService service = serviceMethod.getAnnotation(AddService.class);

            Service serviceInstance;
            if (serviceMethod.getParameterCount() == 0) {
                try {
                    serviceMethod.setAccessible(true);
                    serviceInstance = (Service) serviceMethod.invoke(null);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create service from method " + serviceMethod, e);
                }
            } else {
                try {
                    serviceMethod.setAccessible(true);
                    serviceInstance = (Service) serviceMethod.invoke(null, config);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create service from method " + serviceMethod, e);
                }
            }
            if (service.path().equals(AddService.UNCONFIGURED_PATH)) {
                routing.register(serviceInstance);
            } else {
                routing.register(service.path(), serviceInstance);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T instantiate(Class<T> value, Config config) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        // static method create()
        for (Method declaredMethod : value.getDeclaredMethods()) {
            if (!Modifier.isStatic(declaredMethod.getModifiers())) {
                continue;
            }
            if (!declaredMethod.getName().equals("create")) {
                continue;
            }
            if (declaredMethod.getParameterCount() != 1) {
                continue;
            }
            if (!declaredMethod.getParameterTypes()[0].equals(Config.class)) {
                continue;
            }
            if (!declaredMethod.getReturnType().equals(value)) {
                continue;
            }
            try {
                return (T) lookup.unreflect(declaredMethod)
                        .invoke(config);
            } catch (Throwable throwable) {
                throw new RuntimeException("Failed to invoke static create method on " + value);
            }
        }

        // no arg constructor
        for (Constructor<?> constructor : value.getDeclaredConstructors()) {
            if (constructor.getParameterCount() != 1) {
                continue;
            }
            if (!constructor.getParameterTypes()[0].equals(Config.class)) {
                continue;
            }
            try {
                return (T) lookup.unreflectConstructor(constructor)
                        .invoke(config);
            } catch (Throwable throwable) {
                throw new RuntimeException("Failed to invoke no-arg constructor on " + value);
            }
        }

        throw new RuntimeException(value + " does not have a constructor with Config parameter, "
                                           + "nor a static create(Config) method, cannot instantiate");
    }

    @SuppressWarnings("unchecked")
    private <T> T invokeMethodWithOptionalConfig(Method m) {
        try {
            m.setAccessible(true);
            if (m.getParameterCount() == 0) {
                return (T) m.invoke(null);
            } else {
                return (T) m.invoke(null, config);
            }

        } catch (Throwable throwable) {
            throw new RuntimeException("Failed to invoke method " + m, throwable);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T instantiate(Class<T> value) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        // static method create()
        for (Method declaredMethod : value.getDeclaredMethods()) {
            if (!Modifier.isStatic(declaredMethod.getModifiers())) {
                continue;
            }
            if (!declaredMethod.getName().equals("create")) {
                continue;
            }
            if (declaredMethod.getParameterCount() != 0) {
                continue;
            }
            if (!declaredMethod.getReturnType().equals(value)) {
                continue;
            }
            try {
                return (T) lookup.unreflect(declaredMethod)
                        .invoke();
            } catch (Throwable throwable) {
                throw new RuntimeException("Failed to invoke static create method on " + value);
            }
        }

        // no arg constructor
        for (Constructor<?> constructor : value.getDeclaredConstructors()) {
            if (constructor.getParameterCount() != 0) {
                continue;
            }
            try {
                return (T) lookup.unreflectConstructor(constructor)
                        .invoke();
            } catch (Throwable throwable) {
                throw new RuntimeException("Failed to invoke no-arg constructor on " + value);
            }
        }

        throw new RuntimeException(value + " does not have a no-arg constructor, nor a static create method, cannot instantiate");
    }

    private void registerRouteMethods(Routing.Builder routing, Set<Method> routeMethods) {
        for (Method routeMethod : routeMethods) {
            AddRoute addRoute = routeMethod.getAnnotation(AddRoute.class);

            List<Http.RequestMethod> methods = getMethods(addRoute);
            String path = addRoute.value();

            if (methods.isEmpty()) {
                routing.any(path, toHandler(routeMethod));
            } else {
                routing.anyOf(methods, path, toHandler(routeMethod));
            }
        }
    }

    private Handler toHandler(Method routeMethod) {
        routeMethod.setAccessible(true);
        return (req, res) -> {
            try {
                routeMethod.invoke(null, req, res);
            } catch (Exception e) {
                throw new RuntimeException("Failed to invoke route method", e);
            }
        };
    }

    private List<Http.RequestMethod> getMethods(AddRoute addRoute) {
        return new LinkedList<>(Arrays.asList(addRoute.methods()));
    }

    private void addSetupRoutingMethods(Set<Method> setupRoutingMethods, Method[] methods) {
        // static methods that return Routing
        for (Method method : methods) {
            SetupRouting setup = method.getAnnotation(SetupRouting.class);
            if (setup != null) {
                validateStatic(method, "@SetupRouting");
                validateReturnType(method, Routing.class);
                validateParamsWithOptionalConfig(method, "@SetupRouting");
                setupRoutingMethods.add(method);
            }
        }
    }

    private void addRouteMethods(Set<Method> routeMethods, Set<Method> serviceMethods, Method[] methods) {
        // static methods that have server request and response as params
        for (Method method : methods) {
            if (method.getAnnotation(AddRoute.class) != null) {
                processAddRoute(routeMethods, method);
            } else if (method.getAnnotation(AddService.class) != null) {
                processAddService(serviceMethods, method);
            }
        }
    }

    private void processAddService(Set<Method> serviceMethods, Method method) {
        AddService service = method.getAnnotation(AddService.class);

        validateStatic(method, "@AddService");
        validateReturnType(method, service.value());
        validateParamsWithOptionalConfig(method, "@AddService");

        serviceMethods.add(method);
    }

    private void validateParamsWithOptionalConfig(Method method, String annotation) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            return;
        }
        if (parameterTypes.length != 1) {
            throw new RuntimeException("Method " + method + " is annotated with " + annotation + ", yet it does not have zero "
                                               + " or one parameters.");
        }
        if (parameterTypes[0] != Config.class) {
            throw new RuntimeException("Method " + method + " is annotated with " + annotation + ", yet its parameter is not "
                                               + "Config, but " + parameterTypes[0]);
        }
    }

    private void validateReturnType(Method method, Class<?> expected) {
        Class<?> returnType = method.getReturnType();
        if (!returnType.equals(expected)) {
            throw new RuntimeException("Method " + method + " should return " + expected + ", but returns " + returnType);
        }
    }

    private void validateStatic(Method method, String annotation) {
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new RuntimeException("Method " + method + " is annotated with " + annotation + ", yet it is not static.");
        }
    }

    private void processAddRoute(Set<Method> routeMethods, Method method) {
        validateStatic(method, "@AddRoute");
        validateReturnType(method, Void.TYPE);

        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 2) {
            throw new RuntimeException("Method " + method + " is annotated with @AddRoute, yet it does not have two "
                                               + "parameters: ServerRequest and ServerResponse.");
        }
        if (parameterTypes[0] != ServerRequest.class || parameterTypes[1] != ServerResponse.class) {
            throw new RuntimeException("Method " + method + " is annotated with @AddRoute, yet it does not have two "
                                               + "parameters: ServerRequest and ServerResponse.");
        }

        routeMethods.add(method);
    }

    private static final class ConfigMeta {
        private final Map<String, String> additionalKeys = new HashMap<>();
        private final List<String> additionalSources = new ArrayList<>();

        private ConfigMeta() {
        }

        private void addConfig(AddConfig[] configs) {
            for (AddConfig config : configs) {
                additionalKeys.put(config.key(), config.value());
            }
        }

        private void configuration(Configuration config) {
            if (config == null) {
                return;
            }
            additionalSources.addAll(List.of(config.configSources()));
        }

        ConfigMeta nextMethod() {
            ConfigMeta methodMeta = new ConfigMeta();

            methodMeta.additionalKeys.putAll(this.additionalKeys);
            methodMeta.additionalSources.addAll(this.additionalSources);

            return methodMeta;
        }
    }
}
