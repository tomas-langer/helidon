package io.helidon.inject.tests.javax.inject;

import javax.inject.Singleton;

@Singleton
class UnQualifiedService implements ContractOfQualified {
    @Override
    public String qualifier() {
        return "unqualified";
    }
}
