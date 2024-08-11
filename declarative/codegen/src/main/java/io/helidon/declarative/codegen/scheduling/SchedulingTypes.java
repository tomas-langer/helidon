package io.helidon.declarative.codegen.scheduling;

import io.helidon.common.types.TypeName;

final class SchedulingTypes {
    static final TypeName TASK = TypeName.create("io.helidon.scheduling.Task");
    static final TypeName SCHEDULING = TypeName.create("io.helidon.scheduling.Scheduling");
    static final TypeName FIXED_RATE = TypeName.create("io.helidon.scheduling.FixedRate");
    static final TypeName FIXED_RATE_ANNOTATION = TypeName.create("io.helidon.scheduling.Scheduling.FixedRate");
    static final TypeName FIXED_RATE_INVOCATION = TypeName.create("io.helidon.scheduling.FixedRateInvocation");
    static final TypeName FIXED_RATE_DELAY_TYPE = TypeName.create("io.helidon.scheduling.FixedRate.DelayType");

    static final TypeName CRON = TypeName.create("io.helidon.scheduling.Cron");
    static final TypeName CRON_ANNOTATION = TypeName.create("io.helidon.scheduling.Scheduling.Cron");
    static final TypeName CRON_INVOCATION = TypeName.create("io.helidon.scheduling.CronInvocation");
    private SchedulingTypes() {
    }
}
