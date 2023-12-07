package io.helidon.inject.tests.javax.inject;

import javax.inject.Singleton;

@Singleton
class UnnamedService implements ContractOfNamed {
    @Override
    public String name() {
        return "unnamed";
    }
}
