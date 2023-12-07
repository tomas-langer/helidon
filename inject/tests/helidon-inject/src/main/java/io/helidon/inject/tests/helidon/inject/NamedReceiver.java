package io.helidon.inject.tests.helidon.inject;

import io.helidon.inject.service.Inject;

@Inject.Singleton
class NamedReceiver {
    private final ContractOfNamed named;

    @Inject.Point
    NamedReceiver(@Inject.Named("named") ContractOfNamed named) {
        this.named = named;
    }

    ContractOfNamed named() {
        return named;
    }

}
