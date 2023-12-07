package io.helidon.inject.tests.jakarta.inject;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
class QualifiedReceiver {
    private final ContractOfQualified qualified;

    @Inject
    QualifiedReceiver(@QualifierAnnotation("qualified") ContractOfQualified qualified) {
        this.qualified = qualified;
    }

    ContractOfQualified qualified() {
        return qualified;
    }
}
