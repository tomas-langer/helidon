package io.helidon.scheduling;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.helidon.builder.api.Prototype;

final class FixedRateDecorator implements Prototype.BuilderDecorator<FixedRateConfig.BuilderBase<?, ?>> {
    @SuppressWarnings("removal")
    @Override
    public void decorate(FixedRateConfig.BuilderBase<?, ?> target) {

        // new values are set using the option decorators below, now we can just re-set the deprecated values
        target.initialDelay(target.delayBy().toMillis());
        target.delay(target.rate().map(Duration::toMillis).orElse(1000L));
        target.timeUnit(TimeUnit.MILLISECONDS);
    }

    @SuppressWarnings("removal")
    static final class InitialDelayDecorator implements Prototype.OptionDecorator<FixedRateConfig.BuilderBase<?, ?>, Long> {
        @Override
        public void decorate(FixedRateConfig.BuilderBase<?, ?> builder, Long optionValue) {
            builder.delayBy(Duration.of(optionValue, builder.timeUnit().toChronoUnit()));
        }
    }

    static final class DelayDecorator implements Prototype.OptionDecorator<FixedRateConfig.BuilderBase<?, ?>, Long> {
        @SuppressWarnings("removal")
        @Override
        public void decorate(FixedRateConfig.BuilderBase<?, ?> builder, Long optionValue) {
            builder.rate(Duration.of(optionValue, builder.timeUnit().toChronoUnit()));
        }
    }

    @SuppressWarnings("removal")
    static final class TimeUnitDecorator implements Prototype.OptionDecorator<FixedRateConfig.BuilderBase<?, ?>, TimeUnit> {
        @Override
        public void decorate(FixedRateConfig.BuilderBase<?, ?> builder, TimeUnit optionValue) {
            builder.delay().ifPresent(it -> builder.rate(Duration.of(it, optionValue.toChronoUnit())));
            builder.initialDelay().ifPresent(it -> builder.delayBy(Duration.of(it, optionValue.toChronoUnit())));
        }
    }
}
