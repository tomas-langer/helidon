package io.helidon.inject.tests.jakarta.inject;

import jakarta.inject.Inject;

class NonSingletonService {
    private final SingletonService singleton;

    @Inject
    NonSingletonService(SingletonService singleton) {
        this.singleton = singleton;
    }

    SingletonService singletonService() {
        return singleton;
    }
}
