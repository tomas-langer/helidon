package io.helidon.inject.tests.javax.inject;

import javax.inject.Named;
import javax.inject.Singleton;

@Named("named")
@Singleton
class NamedService implements ContractOfNamed {
    @Override
    public String name() {
        return "named";
    }
}
