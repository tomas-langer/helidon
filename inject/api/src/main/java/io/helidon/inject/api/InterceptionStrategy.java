package io.helidon.inject.api;

/**
 * Possible strategies to interception.
 * Whether interception is supported, and this is honored depends on implementation.
 * <p>
 * The strategy is (in Helidon inject) only honored at compilation time. At runtime, it can only be enabled or disabled.
 */
public enum InterceptionStrategy {
    /**
     * No annotations will qualify in triggering interceptor creation (interception is disabled).
     */
    NONE,
    /**
     * Meta-annotation based. Only annotations annotated with {@link InterceptedTrigger} will
     * qualify.
     * This is the default strategy.
     */
    EXPLICIT,

    /**
     * All annotations marked as {@link java.lang.annotation.RetentionPolicy#RUNTIME} will qualify.
     * Also includes all usages of {@link #EXPLICIT}.
     */
    ALL_RUNTIME,
    /**
     * All annotations marked as {@link java.lang.annotation.RetentionPolicy#RUNTIME} and
     * {@link java.lang.annotation.RetentionPolicy#CLASS} will qualify.
     * Also includes all usages of {@link #EXPLICIT}.
     */
    ALL_RETAINED
}
