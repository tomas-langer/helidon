package io.helidon.nima.faulttolerance;

import java.util.List;

import io.helidon.common.GenericType;

/**
 * A generated service common interface.
 */
@Deprecated
public interface FtMethod {
    /**
     * List of parameters to match an exact method in the case there is more than one method with the same name with a retry.
     *
     * @return list of parameters
     */
    List<GenericType<?>> parameterTypes();
}
