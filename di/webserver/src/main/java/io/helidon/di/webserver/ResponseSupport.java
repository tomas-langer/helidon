package io.helidon.di.webserver;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.webserver.ServerResponse;

import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.type.Argument;
import io.micronaut.core.type.ReturnType;

/**
 * Support for handling reactive responses and reacting on the actual finish of the asynchronous processing.
 *
 * @author Tomas Langer (tomas.langer@oracle.com)
 */
public class ResponseSupport {
    /**
     * Proceeds with the context and based on the parameters and response type invokes success or error handler.
     *
     * Analyses the response and for reactive responses (Helidon and Java) the {@code toRun} is invoked when it really finishes
     * processing (even if asynchronously).
     * <p/>
     * Supported types:
     * <ul>
     *     <li>{@link io.helidon.common.reactive.Single} - the return type must be exactly this, not a subclass</li>
     *     <li>{@link io.helidon.common.reactive.Multi} - the return type must be exactly this, not a subclass</li>
     *     <li>{@link java.util.concurrent.CompletionStage} - or any implementation, including
     *     {@link java.util.concurrent.CompletableFuture}</li>
     *     <li>{@link java.util.concurrent.Flow.Publisher} - the return type must be exactly this, not a subclass</li>
     *     <li>{@link java.lang.Void} and a {@link io.helidon.webserver.ServerResponse} parameter - waits for response
     *      to be sent</li>
     * </ul>
     *
     * Next analyses the parameters, and if any is {@link io.helidon.webserver.ServerResponse}, waits for the response to finis
     *
     * @param context interceptor invocation context
     * @param successHandler to handle success
     * @param errorHandler to handle errors
     *
     * @return the new response to return (may be a different instance of Single or Multi if those were used
     */
    public static Object intercept(MethodInvocationContext<?, ?> context,
                                   Runnable successHandler,
                                   Consumer<Throwable> errorHandler) {

        ReturnType<?> returnType = context.getReturnType();
        Class<?> returnedType = returnType.getType();
        Object response;
        try {
            response = context.proceed();
        } catch (RuntimeException e) {
            errorHandler.accept(e);
            throw e;
        }

        if (Multi.class.equals(returnedType)) {
            // must be exact type
            return handleMulti(response, successHandler, errorHandler);
        } else if (Single.class.equals(returnedType)) {
            // must be exact type
            return handleSingle(response, successHandler, errorHandler);
        } else if (Flow.Publisher.class.equals(returnedType)) {
            // must be exact type
            return handlePublisher(response, successHandler, errorHandler);
        } else if (CompletionStage.class.isAssignableFrom(returnedType)) {
            // can be any subtype, as we can directly return the original response
            return handleCompletionStage(response, successHandler, errorHandler);
        } else if (Void.class.equals(returnedType) || Void.TYPE.equals(returnedType)) {
            Argument<?>[] arguments = context.getArguments();
            for (int i = 0; i < arguments.length; i++) {
                Argument<?> argument = arguments[i];
                if (ServerResponse.class.isAssignableFrom(argument.getType())) {
                    return serverResponse(response, successHandler, errorHandler, context.getParameterValues()[i]);
                }
            }
        }

        successHandler.run();

        return response;
    }

    private static Object serverResponse(Object response, Runnable successHandler, Consumer<Throwable> errorHandler, Object parameter) {
        ServerResponse serverResponse = (ServerResponse) parameter;
        serverResponse.whenSent()
                .thenRun(successHandler)
                .exceptionally(throwable -> {
                    errorHandler.accept(cause(throwable));
                    return null;
                });

        return response;
    }

    private static Object handleCompletionStage(Object response, Runnable successHandler, Consumer<Throwable> errorHandler) {
        CompletionStage<?> stage = (CompletionStage<?>) response;

        stage.thenRun(successHandler)
                .exceptionally(throwable -> {
                    errorHandler.accept(cause(throwable));
                    return null;
                });

        return response;
    }

    private static Object handlePublisher(Object response, Runnable successHandler, Consumer<Throwable> errorHandler) {
        Flow.Publisher<?> pub = (Flow.Publisher<?>) response;
        return Multi.create(pub)
                .onComplete(successHandler)
                .onError(throwable -> errorHandler.accept(cause(throwable)));
    }

    private static Object handleSingle(Object response, Runnable successHandler, Consumer<Throwable> errorHandler) {
        Single<?> single = (Single<?>) response;
        return single.onComplete(successHandler)
                .onError(throwable -> errorHandler.accept(cause(throwable)));
    }

    private static Object handleMulti(Object response, Runnable successHandler, Consumer<Throwable> errorHandler) {
        Multi<?> multi = (Multi<?>) response;
        return multi.onComplete(successHandler)
                .onError(throwable -> errorHandler.accept(cause(throwable)));
    }

    private static Throwable cause(Throwable t) {
        if (t instanceof CompletionException) {
            return cause(t.getCause());
        }
        if (t instanceof ExecutionException) {
            return cause(t.getCause());
        }
        return t;
    }

}
