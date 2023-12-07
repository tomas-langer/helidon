package io.helidon.inject.tests.jakarta.inject;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
class NamedReceiver {
    private final ContractOfNamed named;

    @Inject
    NamedReceiver(@Named("named") ContractOfNamed named) {
        this.named = named;
    }

    ContractOfNamed named() {
        return named;
    }

}
