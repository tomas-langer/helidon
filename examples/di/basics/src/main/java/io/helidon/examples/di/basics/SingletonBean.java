package io.helidon.examples.di.basics;

import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Singleton;

@Singleton
public class SingletonBean {
    private static final AtomicLong COUNTER = new AtomicLong();
    private final long id;

    protected SingletonBean() {
        this.id = COUNTER.incrementAndGet();
    }

    public long getId() {
        return id;
    }
}
