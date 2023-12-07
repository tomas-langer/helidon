package io.helidon.inject.tests.helidon.inject;

import io.helidon.inject.service.Inject;

@Inject.Singleton
class QualifiedReceiver {
    private final ContractOfQualified qualified;

    @Inject.Point
    QualifiedReceiver(@QualifierAnnotation("qualified") ContractOfQualified qualified) {
        this.qualified = qualified;
    }

    ContractOfQualified qualified() {
        return qualified;
    }
}
