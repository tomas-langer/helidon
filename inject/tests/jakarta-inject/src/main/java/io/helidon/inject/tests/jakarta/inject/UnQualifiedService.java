package io.helidon.inject.tests.jakarta.inject;

import jakarta.inject.Singleton;

@Singleton
class UnQualifiedService implements ContractOfQualified {
    @Override
    public String qualifier() {
        return "unqualified";
    }
}
