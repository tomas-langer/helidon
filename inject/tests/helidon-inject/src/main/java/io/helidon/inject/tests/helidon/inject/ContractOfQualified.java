package io.helidon.inject.tests.helidon.inject;

import io.helidon.inject.service.Inject;

@Inject.Contract
interface ContractOfQualified {
    String qualifier();
}
