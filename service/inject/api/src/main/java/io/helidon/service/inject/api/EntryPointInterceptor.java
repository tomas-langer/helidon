package io.helidon.service.inject.api;

import io.helidon.service.registry.Service;

/**
 * Interceptor around initial entry into the Helidon system.
 * <p>
 * The following should be considered entry points:
 * <ul>
 *     <li>HTTP Request on the WebServer (includes grpc, initial web-socket request, graphQL)</li>
 *     <li>Messaging inbound message handled by Helidon component</li>
 *     <li>WebSocket message</li>
 *     <li>Scheduling trigger</li>
 * </ul>
 * The exceptional behavior of this interceptor is that it is invoked exactly once for each entry
 * point. This can be used for resource management, such as when there is a requirement to close
 * resources after the request finishes.
 */
@Service.Contract
public interface EntryPointInterceptor {
    /**
     * Handle interception. This is to make
     * @param chain
     * @param args
     * @return
     * @param <T>
     * @throws Throwable
     */
    <T> T proceed(InvocationContext invocationContext,
                  Interception.Interceptor.Chain<T> chain,
                  Object... args) throws Exception;
}
