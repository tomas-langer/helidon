package io.helidon.declarative.tests.http;

import io.helidon.scheduling.FixedRateInvocation;
import io.helidon.scheduling.Scheduling;
import io.helidon.service.inject.api.Injection;

@Injection.Singleton
class ScheduledTask {
    // every second
    @Scheduling.Cron("* * * * * ?")
    void scheduled() {
        System.out.println("Scheduled cron");
    }

    @Scheduling.FixedRate("PT1S")
    void fixedRate(FixedRateInvocation invocation) {
        System.out.println("Scheduled fixed rate. Iteration: " + invocation.iteration());
    }
}
