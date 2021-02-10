package io.helidon.di;

import io.micronaut.context.event.ApplicationEvent;

public class ContextStartedEvent extends ApplicationEvent {
    public ContextStartedEvent(Object source) {
        super(source);
    }
}
