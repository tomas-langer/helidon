package io.helidon.nima.faulttolerance;

import io.helidon.pico.api.Contract;

/**
 * A generated service to support retries without resorting to Class.forName() for exception types.
 * @deprecated only for generated code
 */
@Contract
@Deprecated
public interface RetryMethod extends FtMethod {
    /**
     * Provide a retry instance that should be used with this method.
     * If the retry annotation contains a name, we will attempt to obtain the named instance from
     * registry. If such a named instance does not exist a new retry will be created from the annotation.
     *
     * @return retry instance
     */
    Retry retry();
}
