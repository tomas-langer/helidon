package io.helidon.di.annotation.http;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import io.micronaut.core.annotation.NonBlocking;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Mark a service as non-blocking.
 * Non-blocking services cna be used without the need of an executor service.
 * <p>
 * This annotation is implicit on methods that return a non-blocking class:
 * <ul>
 *     <li>{@link java.util.concurrent.Flow.Publisher} and its implementations, including Helidon {@code Multi}</li>
 *     <li>{@link java.util.concurrent.CompletionStage} and its implementations, including Helidon {@code Single}</li>
 * </ul>
 * <p>
 * This annotation is also implicit on methods with Helidon WebServer signatures:
 * <ul>
 *     <li>{@code void methodName(ServerRequest, ServerResponse)}</li>
 *     <li>Any method with {@code ServerResponse} parameter</li>
 * </ul>
 *
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.METHOD})
@NonBlocking
public @interface Nonblocking {
}
