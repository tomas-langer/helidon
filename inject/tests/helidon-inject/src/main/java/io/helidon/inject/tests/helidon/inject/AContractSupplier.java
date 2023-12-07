package io.helidon.inject.tests.helidon.inject;

import java.util.function.Supplier;

import io.helidon.inject.service.Inject;

@Inject.Singleton
class AContractSupplier implements Supplier<AContract> {
    @Override
    public AContract get() {
        return () -> "Hello!";
    }
}
