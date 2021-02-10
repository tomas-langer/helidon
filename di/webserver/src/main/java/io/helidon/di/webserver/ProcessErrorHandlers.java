package io.helidon.di.webserver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Singleton;

import io.helidon.di.annotation.http.ErrorHandle;
import io.helidon.webserver.ErrorHandler;

import io.micronaut.context.ExecutionHandleLocator;
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.bind.BoundExecutable;
import io.micronaut.core.bind.DefaultExecutableBinder;
import io.micronaut.core.bind.ExecutableBinder;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.MethodExecutionHandle;

@SuppressWarnings("ALL")
@Internal
@Singleton
class ProcessErrorHandlers extends ProcessRoutes implements ExecutableMethodProcessor<ErrorHandle> {
    private final ExecutableBinder<HttpExchange> binder = new DefaultExecutableBinder<>();
    private final Map<Class, Map<Class, ErrorHandler<Throwable>>> localHandlers = new HashMap<>();
    private final Map<Class, ErrorHandler<Throwable>> globalHandlers = new HashMap<>();
    private final Set<Class> configuredHandlers = new HashSet<>();

    private final RouteBuilders routeBuilders;
    private final ExecutionHandleLocator executionHandleLocator;
    private final HttpBindingRegistry bindingRegistry;

    protected ProcessErrorHandlers(RouteBuilders routeBuilders,
                                   ExecutionHandleLocator executionHandleLocator,
                                   HttpBindingRegistry bindingRegistry) {
        this.routeBuilders = routeBuilders;
        this.executionHandleLocator = executionHandleLocator;
        this.bindingRegistry = bindingRegistry;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        Optional<AnnotationValue<ErrorHandle>> annotation = method.findAnnotation(ErrorHandle.class);
        // this should be present, as otherwise this class should not be called, but to be sure

        if (annotation.isPresent()) {
            boolean global = annotation.get()
                    .booleanValue("global")
                    .orElse(false);

            MethodExecutionHandle<?, Object> executionHandle = executionHandleLocator
                    .createExecutionHandle(beanDefinition, (ExecutableMethod<Object, ?>) method);

            Consumer<HttpExchangeResult> postProcessing = postProcessing(method);

            Class<Throwable> errorClass = (Class<Throwable>) annotation.get()
                    .classValue()
                    .orElse(Throwable.class);

            if (configuredHandlers.add(errorClass)) {
                configureRouting(errorClass);
            }

            if (global) {
                globalHandlers.computeIfAbsent(errorClass, ec -> errorHandler(executionHandle,
                                                                              method,
                                                                              postProcessing(method)));
            } else {
                Class<?> declaringType = executionHandle.getDeclaringType();
                localHandlers.computeIfAbsent(declaringType, type -> new HashMap<>())
                        .computeIfAbsent(errorClass, ec -> errorHandler(executionHandle,
                                                                        method,
                                                                        postProcessing(method)));
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private <V> V findValue(Class actualClass, Map<Class, V> theMap) {
        for (var entry : theMap.entrySet()) {
            if (entry.getKey().isAssignableFrom(actualClass)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void configureRouting(Class<? extends Throwable> errorClass) {
        routeBuilders.errorHandler(errorClass, (req, res, ex) -> {
            Class<Throwable> thrownClass = (Class<Throwable>) ex.getClass();
            Optional<Class> invokedRouteFrom = req.context().get(ProcessRoutes.class, Class.class);
            ErrorHandler<Throwable> handler = null;

            if (invokedRouteFrom.isPresent()) {
                Class invokingType = invokedRouteFrom.get();
                // first try local execution handlers
                var errorToHandler = localHandlers
                        .get(invokingType);
                if (errorToHandler == null) {
                    errorToHandler = findValue(invokingType, localHandlers);
                }

                if (errorToHandler != null) {
                    // we have some local error handling defined
                    // try to get exact match
                    handler = errorToHandler.get(thrownClass);

                    if (handler == null) {
                        handler = findValue(thrownClass, errorToHandler);
                    }
                }
            }

            // let's try global if not found local
            if (handler == null) {
                handler = globalHandlers.get(thrownClass);
                if (handler == null) {
                    handler = findValue(thrownClass, globalHandlers);
                }
            }

            if (handler != null) {
                handler.accept(req, res, ex);
            } else {
                req.next(ex);
            }
        });
    }

    private ErrorHandler<Throwable> errorHandler(MethodExecutionHandle<?, Object> executionHandle,
                                                 ExecutableMethod<?, ?> method,
                                                 Consumer<HttpExchangeResult> postProcessing) {
        return (req, res, ex) -> {
            HttpExchange httpExchange = HttpExchange.create(req, res, ex);
            BoundExecutable<?, ?> boundExecutable = binder.bind(method, bindingRegistry, httpExchange);
            Object methodResult = executionHandle.invoke(boundExecutable.getBoundArguments());
            postProcess(postProcessing, httpExchange, methodResult);
        };
    }
}
