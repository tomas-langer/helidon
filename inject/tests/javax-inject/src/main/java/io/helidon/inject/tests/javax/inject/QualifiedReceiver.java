package io.helidon.inject.tests.javax.inject;

import javax.inject.Inject;
import javax.inject.Singleton;

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
