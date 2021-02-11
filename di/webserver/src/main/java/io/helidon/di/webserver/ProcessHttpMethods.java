package io.helidon.di.webserver;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Singleton;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.di.annotation.http.Entity;
import io.helidon.di.annotation.http.HttpEntry;
import io.helidon.di.annotation.http.HttpMethod;
import io.helidon.di.annotation.http.Path;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;

import io.micronaut.context.BeanContext;
import io.micronaut.context.ExecutionHandleLocator;
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.bind.BoundExecutable;
import io.micronaut.core.bind.DefaultExecutableBinder;
import io.micronaut.core.bind.ExecutableBinder;
import io.micronaut.core.type.Argument;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.MethodExecutionHandle;

@Internal
@Singleton
@Order(ServiceOrder.SERVER)
class ProcessHttpMethods extends ProcessRoutes implements ExecutableMethodProcessor<HttpEntry> {
    private final ExecutableBinder<HttpExchange> binder = new DefaultExecutableBinder<>();

    private final BeanContext beanContext;
    private final Config config;
    private final RouteBuilders routeBuilders;
    private final ExecutionHandleLocator executionHandleLocator;
    private final HttpBindingRegistry bindingRegistry;

    protected ProcessHttpMethods(BeanContext beanContext,
                                 Config config,
                                 RouteBuilders routeBuilders,
                                 ExecutionHandleLocator executionHandleLocator,
                                 HttpBindingRegistry bindingRegistry) {
        this.beanContext = beanContext;
        this.config = config;
        this.routeBuilders = routeBuilders;
        this.executionHandleLocator = executionHandleLocator;
        this.bindingRegistry = bindingRegistry;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void process(BeanDefinition<?> beanDefinitionParam, ExecutableMethod<?, ?> method) {
        Optional<Class<? extends Annotation>> httpEntry = method.getAnnotationTypeByStereotype(HttpEntry.class);

        if (httpEntry.isPresent()) {
            // need to find the correct bean definition
            // as otherwise interceptors do not work
            BeanDefinition<?> beanDefinition = beanContext.getBeanDefinition(beanDefinitionParam.getBeanType());
            MethodExecutionHandle<?, Object> executionHandle = executionHandleLocator
                    .createExecutionHandle(beanDefinition, (ExecutableMethod<Object, ?>) method);

            Class<? extends Annotation> entryAnnotation = httpEntry.get();

            Consumer<HttpExchangeResult> postProcessing = postProcessing(method);

            Http.RequestMethod httpMethod;

            if (entryAnnotation == HttpMethod.class) {
                httpMethod = Http.RequestMethod.create(method.getAnnotationMetadata()
                                                               .stringValue(HttpMethod.class)
                                                               .orElse("GET"));
            } else {
                httpMethod = Http.Method.valueOf(entryAnnotation.getSimpleName().toUpperCase());
            }

            String path = methodPath(beanDefinition, method);

            Optional<Argument<?>> entityType = entityType(method);

            Routing.Builder routingBuilder = RoutingFactory.routingBuilder(config,
                                                                           routeBuilders,
                                                                           beanDefinition.getName(),
                                                                           beanDefinition.getAnnotationMetadata());

            if (entityType.isEmpty()) {
                configureRouting(routingBuilder,
                                 executionHandle,
                                 method,
                                 httpMethod,
                                 path,
                                 postProcessing);
            } else {
                configureRouting(routingBuilder,
                                 executionHandle,
                                 method,
                                 httpMethod,
                                 path,
                                 entityType.get(),
                                 postProcessing);
            }
        }
    }

    private void configureRouting(Routing.Builder routing,
                                  MethodExecutionHandle<?, Object> executionHandle,
                                  ExecutableMethod<?, ?> method,
                                  Http.RequestMethod httpMethod,
                                  String path,
                                  Argument<?> argument,
                                  Consumer<HttpExchangeResult> postProcessing) {

        routing.anyOf(Set.of(httpMethod),
                             path,
                             Handler.create(argument.getType(),
                                            (req, res, entity) -> {
                                                req.context().register(ProcessRoutes.class, executionHandle.getDeclaringType());
                                                HttpExchange httpExchange = HttpExchange.create(req, res, entity);
                                                BoundExecutable<?, ?> boundExecutable = binder
                                                        .bind(method, bindingRegistry, httpExchange);
                                                Object methodResult = executionHandle.invoke(boundExecutable.getBoundArguments());
                                                postProcess(postProcessing, httpExchange, methodResult);
                                            }));

    }

    private void configureRouting(Routing.Builder routing,
                                  MethodExecutionHandle<?, Object> executionHandle,
                                  ExecutableMethod<?, ?> method,
                                  Http.RequestMethod httpMethod,
                                  String path,
                                  Consumer<HttpExchangeResult> postProcessing) {

        routing.anyOf(Set.of(httpMethod), path, (req, res) -> {
            req.context().register(ProcessRoutes.class, executionHandle.getDeclaringType());
            HttpExchange httpExchange = HttpExchange.create(req, res);
            BoundExecutable<?, ?> boundExecutable = binder.bind(method, bindingRegistry, httpExchange);
            Object methodResult = executionHandle.invoke(boundExecutable.getBoundArguments());
            postProcess(postProcessing, httpExchange, methodResult);
        });
    }

    private Optional<Argument<?>> entityType(ExecutableMethod<?, ?> method) {
        for (Argument<?> argument : method.getArguments()) {
            if (argument.getAnnotationMetadata().hasAnnotation(Entity.class)) {
                return Optional.of(argument);
            }
        }
        return Optional.empty();
    }

    private String methodPath(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        String typeName = beanDefinition.getBeanType().getName();
        return RoutingFactory.routingPath(config, typeName, beanDefinition.getAnnotationMetadata())
                .map(it -> joinPath(it, methodPath(typeName, method)))
                .orElseGet(() -> methodPath(typeName, method).orElse("/"));
    }

    private String joinPath(String first, Optional<String> second) {
        String firstValue = first.endsWith("/") ? first.substring(0, first.length() - 1) : first;
        if (second.isEmpty()) {
            return firstValue;
        }
        String secondValue = second.get();
        if (secondValue.startsWith("/")) {
            return firstValue + secondValue;
        }

        return firstValue + "/" + secondValue;
    }

    private Optional<String> methodPath(String className, ExecutableMethod<?, ?> method) {
        return config.get(className + "." + method.getName() + "." + Path.CONFIG_KEY_PATH)
                .asString()
                .or(() -> method.getAnnotationMetadata()
                        .findDeclaredAnnotation(Path.class)
                        .flatMap(AnnotationValue::stringValue));
    }

}
