package io.helidon.inject.tests.javax.inject;

import javax.inject.Inject;

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
