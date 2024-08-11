package io.helidon.scheduling;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

class FixedRateDecorator extends TaskConfigDecorator<FixedRateConfig.BuilderBase<?, ?>> {
    @SuppressWarnings("removal")
    @Override
    public void decorate(FixedRateConfig.BuilderBase<?, ?> target) {
        super.decorate(target);

        // transform deprecated options to supported options and vice versa
        if (target.delay().isPresent() && target.delayBy().isZero()) {
            target.rate(Duration.of(target.delay().get(), target.timeUnit().toChronoUnit()));
        }
        if (target.initialDelay().isPresent() && target.delayBy().isZero()) {
            target.delayBy(Duration.of(target.initialDelay().get(), target.timeUnit().toChronoUnit()));
        }

        // set the deprecated values to correct values. If "rate" is missing, normal validation will kick-in
        target.initialDelay(target.delayBy().toMillis());
        target.delay(target.rate().map(Duration::toMillis).orElse(1000L));
        target.timeUnit(TimeUnit.MILLISECONDS);
    }
}
