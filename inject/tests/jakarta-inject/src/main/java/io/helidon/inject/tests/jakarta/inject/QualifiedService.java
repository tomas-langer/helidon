package io.helidon.inject.tests.jakarta.inject;

import jakarta.inject.Singleton;

@QualifierAnnotation("qualified")
@Singleton
class QualifiedService implements ContractOfQualified {
    @Override
    public String qualifier() {
        return "qualified";
    }
}
