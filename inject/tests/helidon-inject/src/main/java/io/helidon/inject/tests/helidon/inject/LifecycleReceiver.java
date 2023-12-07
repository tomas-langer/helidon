package io.helidon.inject.tests.helidon.inject;

import io.helidon.inject.service.Inject;

@Inject.Singleton
class LifecycleReceiver {
    private boolean postConstructCalled;
    private boolean preDestroyCalled;

    LifecycleReceiver() {
    }

    @Inject.PostConstruct
    void postConstruct() {
        this.postConstructCalled = true;
    }

    @Inject.PreDestroy
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
