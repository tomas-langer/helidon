package io.helidon.mn.webserver;

import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

import javax.inject.Singleton;

import io.helidon.annotation.http.Controller;
import io.helidon.annotation.http.Entity;
import io.helidon.annotation.http.Error;
import io.helidon.annotation.http.HttpEntry;
import io.helidon.annotation.http.HttpMethod;
import io.helidon.annotation.http.Path;
import io.helidon.annotation.http.Status;
import io.helidon.annotation.http.StatusCode;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Http;
import io.helidon.webserver.Handler;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerResponse;

import io.micronaut.context.ExecutionHandleLocator;
import io.micronaut.context.processor.ExecutableMethodProcessor;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.bind.BoundExecutable;
import io.micronaut.core.bind.DefaultExecutableBinder;
import io.micronaut.core.bind.ExecutableBinder;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.ReturnType;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import io.micronaut.inject.MethodExecutionHandle;

@Internal
@Singleton
public class MethodRouteBuilder implements ExecutableMethodProcessor<Controller> {
    private final Routing.Builder defaultRouting;
    private final ExecutionHandleLocator executionHandleLocator;
    private final HttpBindingRegistry bindingRegistry;
    private final ExecutableBinder<HttpExchange> binder = new DefaultExecutableBinder<>();

    protected MethodRouteBuilder(Routing.Builder defaultRouting,
                                 ExecutionHandleLocator executionHandleLocator,
                                 HttpBindingRegistry bindingRegistry) {
        this.defaultRouting = defaultRouting;
        this.executionHandleLocator = executionHandleLocator;
        this.bindingRegistry = bindingRegistry;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void process(BeanDefinition<?> beanDefinition, ExecutableMethod<?, ?> method) {
        Optional<Class<? extends Annotation>> httpEntry = method.getAnnotationTypeByStereotype(HttpEntry.class);

        if (httpEntry.isPresent()) {
            MethodExecutionHandle<?, Object> executionHandle = executionHandleLocator
                    .createExecutionHandle(beanDefinition, (ExecutableMethod<Object, ?>) method);

            Class<? extends Annotation> entryAnnotation = httpEntry.get();

            Consumer<HttpExchangeResult> postProcessing = postProcessing(method);

            if (entryAnnotation == Error.class) {
                Class<? extends Throwable> errorClass = method.getAnnotationMetadata()
                        .classValue(Error.class)
                        .orElse(Throwable.class);

                configureErrorRouting(defaultRouting,
                                      executionHandle,
                                      method,
                                      postProcessing,
                                      errorClass);
            } else {
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

                if (entityType.isEmpty()) {
                    configureRouting(defaultRouting,
                                     executionHandle,
                                     method,
                                     httpMethod,
                                     path,
                                     postProcessing);
                } else {
                    configureRouting(defaultRouting,
                                     executionHandle,
                                     method,
                                     httpMethod,
                                     path,
                                     entityType.get(),
                                     postProcessing);
                }
            }

        }
    }

    private void configureErrorRouting(Routing.Builder defaultRouting,
                                       MethodExecutionHandle<?, Object> executionHandle,
                                       ExecutableMethod<?, ?> method,
                                       Consumer<HttpExchangeResult> postProcessing,
                                       Class<? extends Throwable> errorClass) {
        defaultRouting.error(errorClass, (req, res, ex) -> {
            HttpExchange httpExchange = HttpExchange.create(req, res, ex);
            BoundExecutable<?, ?> boundExecutable = binder.bind(method, bindingRegistry, httpExchange);
            Object methodResult = executionHandle.invoke(boundExecutable.getBoundArguments());
            postProcess(postProcessing, httpExchange, methodResult);
        });
    }

    private void configureRouting(Routing.Builder defaultRouting,
                                  MethodExecutionHandle<?, Object> executionHandle,
                                  ExecutableMethod<?, ?> method,
                                  Http.RequestMethod httpMethod,
                                  String path,
                                  Argument<?> argument,
                                  Consumer<HttpExchangeResult> postProcessing) {

        defaultRouting.anyOf(Set.of(httpMethod),
                             path,
                             Handler.create(argument.getType(),
                                            (req, res, entity) -> {
                                                HttpExchange httpExchange = HttpExchange.create(req, res, entity);
                                                BoundExecutable<?, ?> boundExecutable = binder
                                                        .bind(method, bindingRegistry, httpExchange);
                                                Object methodResult = executionHandle.invoke(boundExecutable.getBoundArguments());
                                                postProcess(postProcessing, httpExchange, methodResult);
                                            }));

    }

    private void postProcess(Consumer<HttpExchangeResult> postProcessing, HttpExchange httpExchange, Object methodResult) {
        postProcessing.accept(HttpExchangeResult.create(httpExchange, methodResult));
    }

    private void configureRouting(Routing.Builder defaultRouting,
                                  MethodExecutionHandle<?, Object> executionHandle,
                                  ExecutableMethod<?, ?> method,
                                  Http.RequestMethod httpMethod,
                                  String path,
                                  Consumer<HttpExchangeResult> postProcessing) {

        defaultRouting.anyOf(Set.of(httpMethod), path, (req, res) -> {
            HttpExchange httpExchange = HttpExchange.create(req, res);
            BoundExecutable<?, ?> boundExecutable = binder.bind(method, bindingRegistry, httpExchange);
            Object methodResult = executionHandle.invoke(boundExecutable.getBoundArguments());
            postProcess(postProcessing, httpExchange, methodResult);
        });
    }

    @SuppressWarnings("unchecked")
    Consumer<HttpExchangeResult> postProcessing(ExecutableMethod<?, ?> method) {
        List<Consumer<HttpExchangeResult>> processors = new LinkedList<>();

        // status handling
        Optional<Integer> statusCode;
        if (method.hasAnnotation(Status.class)) {
            statusCode = method.enumValue(Status.class, Http.Status.class)
                    .map(Http.Status::code);
        } else if (method.hasAnnotation(StatusCode.class)) {
            OptionalInt optionalInt = method.intValue(StatusCode.class);
            if (optionalInt.isPresent()) {
                statusCode = Optional.of(optionalInt.getAsInt());
            } else {
                statusCode = Optional.empty();
            }
        } else {
            statusCode = Optional.empty();
        }
        if (statusCode.isPresent()) {
            int status = statusCode.get();
            processors.add(it -> it.response().status(status));
        }

        // entity handling
        ReturnType<?> returnType = method.getReturnType();

        if (returnType.isVoid()) {
            boolean send = true;
            // if parameter contains response, do not send!
            for (Argument<?> argument : method.getArguments()) {
                if (argument.getType() == ServerResponse.class) {
                    send = false;
                }
            }
            // otherwise send without an entity
            if (send) {
                processors.add(it -> it.response().send());
            }
        } else {
            if (CompletionStage.class.isAssignableFrom(returnType.getType())) {
                processors.add(it -> {
                    ((CompletionStage<?>) it.methodReturn())
                            .thenAccept(it.response()::send)
                            .exceptionally(it.response()::send);
                });
            } else if (Flow.Publisher.class.isAssignableFrom(returnType.getType())) {
                // Single/Multi - if DataChunk, just send
                //  otherwise send with type
                Optional<Class<?>> publisherType = returnType.getFirstTypeVariable()
                        .map(Argument::getType);
                if (publisherType.isPresent()) {
                    Class<?> aClass = publisherType.get();
                    if (DataChunk.class.isAssignableFrom(aClass)) {
                        processors.add(it -> it.response().send((Flow.Publisher<DataChunk>) it.methodReturn()));
                    } else {
                        Class<Object> theClass = (Class<Object>) aClass;
                        processors.add(it -> it.response().send((Flow.Publisher<Object>) it.methodReturn(), theClass));
                    }
                } else {
                    processors.add(it -> it.response().send(it.methodReturn()));
                }
            } else {
                // any other type - just send
                processors.add(it -> it.response().send(it.methodReturn()));
            }
        }

        return it -> processors.forEach(processor -> processor.accept(it));
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
        return beanDefinition.getAnnotationMetadata()
                .stringValue(Path.class)
                .map(it -> joinPath(it, methodPath(method)))
                .orElseGet(() -> methodPath(method).orElse("/"));
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

    private Optional<String> methodPath(ExecutableMethod<?, ?> method) {
        return method.getAnnotationMetadata()
                .findDeclaredAnnotation(Path.class)
                .flatMap(AnnotationValue::stringValue);
    }

}
