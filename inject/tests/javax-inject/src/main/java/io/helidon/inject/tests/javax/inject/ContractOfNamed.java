package io.helidon.inject.tests.javax.inject;

import io.helidon.inject.service.Inject;

@Inject.Contract
interface ContractOfNamed {
    String name();
}
