package io.helidon.webserver.http;

import java.util.List;
import java.util.Optional;

import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.service.registry.Service;

/**
 * A filter executed after we find the matching service method in Helidon Declarative.
 * <p>
 * This is ignored when using imperative style of programming, as you have full control of your
 * endpoints.
 */
@Service.Contract
public interface EntryPointFilter {
    /**
     * Filter an invocation of a method, after we know which endpoint will be invoked.
     *
     * @param filterChain call {@link io.helidon.webserver.http.FilterChain#proceed()} to invoke the endpoint (or next filter)
     * @param ctx entry point context
     */
    void filter(FilterChain filterChain,
                EntryPointContext ctx);

    interface EntryPointContext {
        ServerRequest request();
        ServerResponse response();
        TypeName declaringType();
        String methodName();
        List<TypeName> parameterTypes();
    }
}
