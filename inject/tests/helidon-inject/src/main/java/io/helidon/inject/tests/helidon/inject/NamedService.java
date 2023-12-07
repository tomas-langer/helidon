package io.helidon.inject.tests.helidon.inject;

import io.helidon.inject.service.Inject;

@Inject.Named("named")
@Inject.Singleton
class NamedService implements ContractOfNamed {
    @Override
    public String name() {
        return "named";
    }
}
