package io.helidon.inject.codegen.spi;

import io.helidon.codegen.CodegenContext;

/**
 * A {@link java.util.ServiceLoader} provider interface to customize assignments.
 *
 * @see io.helidon.inject.codegen.spi.InjectAssignment
 */
public interface InjectAssignmentProvider {
    /**
     * Create a new provider to customize assignments.
     *
     * @param ctx code generation context
     * @return a new assignment
     */
    InjectAssignment create(CodegenContext ctx);
}
