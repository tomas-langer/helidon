package io.helidon.inject.tests.helidon.inject;

import io.helidon.inject.service.Inject;

@Inject.Singleton
class UnnamedService implements ContractOfNamed {
    @Override
    public String name() {
        return "unnamed";
    }
}
