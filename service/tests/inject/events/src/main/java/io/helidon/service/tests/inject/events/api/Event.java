package io.helidon.service.tests.inject.events.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.concurrent.CompletionStage;

import io.helidon.service.inject.api.Qualifier;
import io.helidon.service.registry.Service;

/**
 * Injection event types.
 * <p>
 * To publish an event, inject an instance of {@link io.helidon.service.tests.inject.events.api.Event.Emitter}.
 * <p>
 * To receive events, implement a method (at least package private) with the event object as a parameter, annotated with
 * {@link io.helidon.service.tests.inject.events.api.Event.Observer}. The method can have any name, must be {@code void},
 * and have a single parameter.
 */
public final class Event {
    private Event() {
    }

    /**
     * A service method that is an event observer. The method MUST have a parameter that is the type of the event.
     */
    @Target(ElementType.METHOD)
    public @interface Observer {
    }

    /**
     * A service method that is an event observer. The method MUST have a parameter that is the type of the event.
     * Async observers are invoked on a separate thread and will never feed information back to the
     * {@link io.helidon.service.tests.inject.events.api.Event.Emitter}
     * (even if {@link Emitter#emitAsync(Object, io.helidon.service.inject.api.Qualifier...)} is used).
     */
    @Target({ElementType.METHOD, ElementType.TYPE})
    public @interface AsyncObserver {
    }

    /**
     * To publish an event, simply inject an instance of this type (correctly typed with your event object) into your service,
     * and call {@link #emit(Object, io.helidon.service.inject.api.Qualifier...)} on it when needed.
     * <p>
     * A single service can inject more than one instance, if it wants to publish events of different types.
     * The type of the event is determining which events are published (i.e. qualifiers or any other annotation are not relevant,
     * only the type of the object).
     *
     * @param <T> type of the event object
     */
    @Service.Contract
    public interface Emitter<T> {
        /**
         * Emit an event.
         * The method blocks until all observers are processed.
         * <p>
         * Only observers with the same qualifiers as specified will be notified.
         *
         * @param eventObject event object to deliver to the observers
         * @param qualifiers  qualifiers (zero or more) that qualify this event instance
         * @throws java.lang.RuntimeException if any exception is encountered, it is re-thrown
         */
        void emit(T eventObject, Qualifier... qualifiers);

        /**
         * Emit an event.
         * The method returns immediately with a completion stage that will get completed
         * when all observers are notified. If any observer throws an exception, it will be added as
         * a suppressed exception when {@link java.util.concurrent.CompletionStage#exceptionally(java.util.function.Function)}
         * is invoked.
         * <p>
         * Only observers with the same qualifiers as specified will be notified.
         *
         * @param eventObject event object to deliver to the listeners
         * @param qualifiers  qualifiers (zero or more) that qualify this event instance
         * @return completion stage to observe completion events
         */
        CompletionStage<T> emitAsync(T eventObject, Qualifier... qualifiers);
    }
}
