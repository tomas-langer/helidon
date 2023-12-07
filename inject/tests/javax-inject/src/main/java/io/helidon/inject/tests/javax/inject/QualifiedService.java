package io.helidon.inject.tests.javax.inject;

import javax.inject.Singleton;

@QualifierAnnotation("qualified")
@Singleton
class QualifiedService implements ContractOfQualified {
    @Override
    public String qualifier() {
        return "qualified";
    }
}
