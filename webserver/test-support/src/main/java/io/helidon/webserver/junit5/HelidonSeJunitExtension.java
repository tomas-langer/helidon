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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.webclient.WebClient;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import io.helidon.webserver.WebServer;

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
    }

    private final List<AddService> classLevelServices = new ArrayList<>();
    private final List<AddMedia> classLevelMedia = new ArrayList<>();
    private final ConfigMeta classLevelConfigMeta = new ConfigMeta();

    private Class<?> testClass;
    private Config config;
    private Config methodConfig;
    private WebServer webServer;
    private WebClient webClient;

    @SuppressWarnings("unchecked")
    @Override
    public void beforeAll(ExtensionContext context) {
        testClass = context.getRequiredTestClass();

        AddConfig[] configs = testClass.getAnnotationsByType(AddConfig.class);
        classLevelConfigMeta.addConfig(configs);
        classLevelConfigMeta.configuration(testClass.getAnnotation(Configuration.class));

        AddService[] services = testClass.getAnnotationsByType(AddService.class);
        classLevelServices.addAll(Arrays.asList(services));

        AddMedia[] media = testClass.getAnnotationsByType(AddMedia.class);
        classLevelMedia.addAll(Arrays.asList(media));

        config = configure(classLevelConfigMeta);

        startServer(classLevelServices, classLevelMedia);

        webClient = WebClient.builder()
                .config(config.get("webclient"))
                .baseUri("http://localhost:" + webServer.port())
                .build();
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
        stopServer();
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
            return webClient;
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

    private void startServer(List<AddService> services,
                             List<AddMedia> media) {

        Set<Method> routeMethods = new HashSet<>();
        Set<Method> serviceMethods = new HashSet<>();
        addRouteMethods(routeMethods, serviceMethods, testClass.getMethods());
        addRouteMethods(routeMethods, serviceMethods, testClass.getDeclaredMethods());

        WebServer.Builder builder = WebServer.builder();
        Routing.Builder routing = Routing.builder();

        registerRouteMethods(routing, routeMethods);
        registerServices(routing, serviceMethods, services);
        registerMedia(builder, media);

        webServer = builder
                .routing(routing)
                .build()
                .start()
                .await(10, TimeUnit.SECONDS);

    }

    private void registerMedia(WebServer.Builder builder, List<AddMedia> media) {
        for (AddMedia addMedia : media) {
            builder.addMediaSupport(instantiate(addMedia.value()));
        }

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
            routing.register(service.path(), serviceInstance);
        }
        for (Method serviceMethod : serviceMethods) {
            AddService service = serviceMethod.getAnnotation(AddService.class);

            Service serviceInstance;
            if (serviceMethod.getParameterCount() == 0) {
                try {
                    serviceInstance = (Service) serviceMethod.invoke(null);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create service from method " + serviceMethod, e);
                }
            } else {
                try {
                    serviceInstance = (Service) serviceMethod.invoke(null, config);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create service from method " + serviceMethod, e);
                }
            }
            routing.register(service.path(), serviceInstance);
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

        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            serviceMethods.add(method);
        }
        if (parameterTypes.length != 1) {
            throw new RuntimeException("Method " + method + " is annotated with @AddService, yet it does not have zero "
                                               + " or one parameters.");
        }
        if (parameterTypes[0] != Config.class) {
            throw new RuntimeException("Method " + method + " is annotated with @AddService, yet its parameter is not "
                                               + "Config, but " + parameterTypes[0]);
        }

        serviceMethods.add(method);
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

    private void stopServer() {
        if (webServer != null) {
            webServer.shutdown()
                    .await(10, TimeUnit.SECONDS);
            webServer = null;
        }
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
