package io.helidon.di.webserver;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

import io.helidon.di.annotation.http.Status;
import io.helidon.di.annotation.http.StatusCode;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.Http;
import io.helidon.webserver.ServerResponse;

import io.micronaut.core.type.Argument;
import io.micronaut.core.type.ReturnType;
import io.micronaut.inject.ExecutableMethod;

abstract class ProcessRoutes {
    protected void postProcess(Consumer<HttpExchangeResult> postProcessing, HttpExchange httpExchange, Object methodResult) {
        postProcessing.accept(HttpExchangeResult.create(httpExchange, methodResult));
    }

    @SuppressWarnings("unchecked")
    protected Consumer<HttpExchangeResult> postProcessing(ExecutableMethod<?, ?> method) {
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
}
