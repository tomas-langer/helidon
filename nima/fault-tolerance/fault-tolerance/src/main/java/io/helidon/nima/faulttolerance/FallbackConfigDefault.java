package io.helidon.nima.faulttolerance;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Concrete implementation w/ builder for {@link io.helidon.nima.faulttolerance.FallbackConfig}.
 *
 * @param <T> return type of the fallback
 */
public class FallbackConfigDefault<T> implements FallbackConfig<T> {
    private final Function<Throwable, ? extends T> fallback;
    private final Set<Class<? extends Throwable>> skipOn;
    private final Set<Class<? extends Throwable>> applyOn;

    private FallbackConfigDefault(Builder<T> b) {
        this.fallback = b.fallback;
        this.skipOn = Collections.unmodifiableSet(new LinkedHashSet<>(b.skipOn));
        this.applyOn = Collections.unmodifiableSet(new LinkedHashSet<>(b.applyOn));
    }

    /**
     * Creates a builder for this type.
     *
     * @return a builder for {@link io.helidon.nima.faulttolerance.FallbackConfig}
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Creates a builder for this type, initialized with the attributes from the values passed.
     *
     * @param val the value to copy to initialize the builder attributes
     * @return a builder for {@link io.helidon.nima.faulttolerance.FallbackConfig}
     */
    public static <T> Builder<T> toBuilder(FallbackConfig<T> val) {
        Objects.requireNonNull(val);
        return FallbackConfigDefault.<T>builder().accept(val);
    }

    @Override
    public Function<Throwable, ? extends T> fallback() {
        return fallback;
    }

    @Override
    public Set<Class<? extends Throwable>> skipOn() {
        return skipOn;
    }

    @Override
    public Set<Class<? extends Throwable>> applyOn() {
        return applyOn;
    }

    /**
     * Fluent API builder for {@link io.helidon.nima.faulttolerance.FallbackConfig}.
     * @param <T> return type of fallback method
     */
    public static class Builder<T> implements io.helidon.common.Builder<Builder<T>, FallbackConfig<T>>, FallbackConfig<T> {
        /**
         * Field value for {@code java.util.Set<java.lang.Class<? extends java.lang.Throwable>> skipOn()}.
         */
        protected final Set<Class<? extends Throwable>> skipOn = new LinkedHashSet<>();
        /**
         * Field value for {@code java.util.Set<java.lang.Class<? extends java.lang.Throwable>> applyOn()}.
         */
        protected final Set<Class<? extends Throwable>> applyOn = new LinkedHashSet<>();

        /**
         * Field value for {@code java.util.function.Function<java.lang.Throwable, ? extends T> fallback()}.
         */
        private Function<Throwable, ? extends T> fallback;

        private Builder() {
        }

        @Override
        public FallbackConfig<T> build() {
            return new FallbackConfigDefault<>(this);
        }

        @Override
        public Function<Throwable, ? extends T> fallback() {
            return fallback;
        }

        @Override
        public Set<Class<? extends Throwable>> skipOn() {
            return skipOn;
        }

        @Override
        public Set<Class<? extends Throwable>> applyOn() {
            return applyOn;
        }

        /**
         * Setter for 'fallback'.
         *
         * @param val the new value
         * @return this fluent builder
         */
        public Builder<T> fallback(Function<Throwable, ? extends T> val) {
            this.fallback = Objects.requireNonNull(val);
            return identity();
        }

        /**
         * Setter for 'skipOn'.
         *
         * @param val the new value
         * @return this fluent builder
         */
        public Builder<T> skipOn(Collection<Class<? extends Throwable>> val) {
            this.skipOn.clear();
            this.skipOn.addAll(Objects.requireNonNull(val));
            return identity();
        }

        /**
         * Setter for 'skipOn'.
         *
         * @param val the new value
         * @return this fluent builder
         */
        public Builder<T> addSkipOn(Collection<Class<? extends Throwable>> val) {
            this.skipOn.addAll(Objects.requireNonNull(val));
            return identity();
        }

        /**
         * Setter for 'skipOn'.
         *
         * @param val the new value
         * @return this fluent builder
         */
        public Builder<T> addSkipOn(Class<? extends Throwable> val) {
            Objects.requireNonNull(val);
            this.skipOn.add(val);
            return identity();
        }

        /**
         * Setter for 'applyOn'.
         *
         * @param val the new value
         * @return this fluent builder
         */
        public Builder<T> applyOn(Collection<Class<? extends Throwable>> val) {
            this.applyOn.clear();
            this.applyOn.addAll(Objects.requireNonNull(val));
            return identity();
        }

        /**
         * Setter for 'applyOn'.
         *
         * @param val the new value
         * @return this fluent builder
         */
        public Builder<T> addApplyOn(Collection<Class<? extends Throwable>> val) {
            this.applyOn.addAll(Objects.requireNonNull(val));
            return identity();
        }

        /**
         * Setter for 'applyOn'.
         *
         * @param val the new value
         * @return this fluent builder
         */
        public Builder<T> addApplyOn(Class<? extends Throwable> val) {
            Objects.requireNonNull(val);
            this.applyOn.add(val);
            return identity();
        }

        /**
         * Accept and update from the provided value object.
         *
         * @param val the value object to copy from
         * @return this instance typed to correct type
         */
        public Builder<T> accept(FallbackConfig<T> val) {
            Objects.requireNonNull(val);
            __acceptThis(val);
            return identity();
        }

        private void __acceptThis(FallbackConfig<T> val) {
            Objects.requireNonNull(val);
            fallback(val.fallback());
            skipOn(val.skipOn());
            applyOn(val.applyOn());
        }
     }
}
