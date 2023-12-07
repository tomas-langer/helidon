package io.helidon.inject.tests.jakarta.inject;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Named("named")
@Singleton
class NamedService implements ContractOfNamed {
    @Override
    public String name() {
        return "named";
    }
}
