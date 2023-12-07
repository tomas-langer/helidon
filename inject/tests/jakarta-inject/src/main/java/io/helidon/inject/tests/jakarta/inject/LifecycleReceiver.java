package io.helidon.inject.tests.jakarta.inject;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;

@Singleton
class LifecycleReceiver {
    private boolean postConstructCalled;
    private boolean preDestroyCalled;

    LifecycleReceiver() {
    }

    @PostConstruct
    void postConstruct() {
        this.postConstructCalled = true;
    }

    @PreDestroy
    void preDestroy() {
        this.preDestroyCalled = true;
    }

    boolean postConstructCalled() {
        return postConstructCalled;
    }

    boolean preDestroyCalled() {
        return preDestroyCalled;
    }
}
