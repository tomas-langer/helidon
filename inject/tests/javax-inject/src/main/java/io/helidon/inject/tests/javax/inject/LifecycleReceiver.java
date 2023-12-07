package io.helidon.inject.tests.javax.inject;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

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
