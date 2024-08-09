package io.helidon.declarative.codegen.faulttolerance;

import io.helidon.common.types.TypeName;

final class FtTypes {
    static final TypeName ERROR_CHECKER = TypeName.create("io.helidon.faulttolerance.ErrorChecker");

    static final TypeName FALLBACK_ANNOTATION = TypeName.create("io.helidon.faulttolerance.FaultTolerance.Fallback");
    static final TypeName FALLBACK_GENERATED_METHOD =
            TypeName.create("io.helidon.faulttolerance.FaultToleranceGenerated.FallbackMethod");

    static final TypeName RETRY_ANNOTATION = TypeName.create("io.helidon.faulttolerance.FaultTolerance.Retry");
    static final TypeName RETRY = TypeName.create("io.helidon.faulttolerance.Retry");
    static final TypeName RETRY_CONFIG = TypeName.create("io.helidon.faulttolerance.RetryConfig");
    static final TypeName RETRY_GENERATED_METHOD =
            TypeName.create("io.helidon.faulttolerance.FaultToleranceGenerated.RetryMethod");

    static final TypeName CIRCUIT_BREAKER_ANNOTATION =
            TypeName.create("io.helidon.faulttolerance.FaultTolerance.CircuitBreaker");
    static final TypeName CIRCUIT_BREAKER = TypeName.create("io.helidon.faulttolerance.CircuitBreaker");
    static final TypeName CIRCUIT_BREAKER_CONFIG = TypeName.create("io.helidon.faulttolerance.CircuitBreakerConfig");
    static final TypeName CIRCUIT_BREAKER_GENERATED_METHOD =
            TypeName.create("io.helidon.faulttolerance.FaultToleranceGenerated.CircuitBreakerMethod");

    private FtTypes() {
    }
}
