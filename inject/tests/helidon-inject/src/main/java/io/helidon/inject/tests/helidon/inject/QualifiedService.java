package io.helidon.inject.tests.helidon.inject;

import io.helidon.inject.service.Inject;

@QualifierAnnotation("qualified")
@Inject.Singleton
class QualifiedService implements ContractOfQualified {
    @Override
    public String qualifier() {
        return "qualified";
    }
}
