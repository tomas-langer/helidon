package io.helidon.inject.tests.helidon.inject;

import io.helidon.inject.service.Inject;

@Inject.Service
class NonSingletonService {
    private final SingletonService singleton;

    @Inject.Point
    NonSingletonService(SingletonService singleton) {
        this.singleton = singleton;
    }

    SingletonService singletonService() {
        return singleton;
    }
}
