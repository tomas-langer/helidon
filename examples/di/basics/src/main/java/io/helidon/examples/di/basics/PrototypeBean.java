package io.helidon.examples.di.basics;

import java.util.concurrent.atomic.AtomicLong;

import io.micronaut.context.annotation.Prototype;

@Prototype
public class PrototypeBean {
    private static final AtomicLong COUNTER = new AtomicLong();
    private final long id;

    protected PrototypeBean() {
        this.id = COUNTER.incrementAndGet();
    }

    public long getId() {
        return id;
    }
}
