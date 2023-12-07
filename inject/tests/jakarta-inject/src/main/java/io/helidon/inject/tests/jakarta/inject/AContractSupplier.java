package io.helidon.inject.tests.jakarta.inject;

import jakarta.inject.Provider;
import jakarta.inject.Singleton;

@Singleton
class AContractSupplier implements Provider<AContract> {
    @Override
    public AContract get() {
        return () -> "Hello!";
    }
}
