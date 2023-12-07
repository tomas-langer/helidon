package io.helidon.inject.tests.jakarta.inject;

import jakarta.inject.Singleton;

@Singleton
class UnnamedService implements ContractOfNamed {
    @Override
    public String name() {
        return "unnamed";
    }
}
