package io.helidon.inject.tests.helidon.inject;

import io.helidon.inject.service.Inject;

@Inject.Singleton
class UnQualifiedService implements ContractOfQualified {
    @Override
    public String qualifier() {
        return "unqualified";
    }
}
