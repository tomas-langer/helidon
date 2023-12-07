package io.helidon.inject.tests.jakarta.inject;

import io.helidon.inject.service.Inject;

@Inject.Contract
interface AContract {
    String message();
}
