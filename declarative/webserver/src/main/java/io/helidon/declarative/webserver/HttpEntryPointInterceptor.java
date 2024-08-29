package io.helidon.declarative.webserver;

import io.helidon.service.inject.api.Interception;
import io.helidon.service.inject.api.InvocationContext;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

@Service.Contract
public interface HttpEntryPointInterceptor {
    void proceed(InvocationContext invocationContext,
                 Chain chain,
                 ServerRequest request,
                 ServerResponse response) throws Exception;

    interface Chain extends Interception.Interceptor.Chain<Void> {
        @Override
        default Void proceed(Object[] args) throws Exception {
            proceed((ServerRequest) args[0], (ServerResponse) args[1]);
            return null;
        }

        void proceed(ServerRequest request, ServerResponse response) throws Exception;
    }
}
