package io.helidon.inject.tests.javax.inject;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

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
