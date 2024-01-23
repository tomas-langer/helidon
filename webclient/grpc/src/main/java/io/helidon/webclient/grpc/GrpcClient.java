package io.helidon.webclient.grpc;

import java.util.function.Consumer;

import io.helidon.builder.api.RuntimeType;
import io.helidon.webclient.api.WebClient;
import io.helidon.webclient.spi.Protocol;

/**
 * gRPC client.
 */
@RuntimeType.PrototypedBy(GrpcClientConfig.class)
public interface GrpcClient extends RuntimeType.Api<GrpcClientConfig> {
    /**
     * Protocol to use to obtain an instance of gRPC specific client from
     * {@link io.helidon.webclient.api.WebClient#client(io.helidon.webclient.spi.Protocol)}.
     */
    Protocol<GrpcClient, GrpcClientProtocolConfig> PROTOCOL = GrpcProtocolProvider::new;

    /**
     * A new fluent API builder to customize client setup.
     *
     * @return a new builder
     */
    static GrpcClientConfig.Builder builder() {
        return GrpcClientConfig.builder();
    }

    /**
     * Create a new instance with custom configuration.
     *
     * @param clientConfig HTTP/2 client configuration
     * @return a new HTTP/2 client
     */
    static GrpcClient create(GrpcClientConfig clientConfig) {
        return new GrpcClientImpl(WebClient.create(it -> it.from(clientConfig)), clientConfig);
    }

    /**
     * Create a new instance customizing its configuration.
     *
     * @param consumer HTTP/2 client configuration
     * @return a new HTTP/2 client
     */
    static GrpcClient create(Consumer<GrpcClientConfig.Builder> consumer) {
        return create(GrpcClientConfig.builder()
                              .update(consumer)
                              .buildPrototype());
    }

    /**
     * Create a new instance with default configuration.
     *
     * @return a new HTTP/2 client
     */
    static GrpcClient create() {
        return create(GrpcClientConfig.create());
    }

    /**
     * Create a client for a specific service. The client will be backed by the same HTTP/2 client.
     *
     * @param descriptor descriptor to use
     * @return client for the provided descriptor
     */
    GrpcServiceClient serviceClient(GrpcServiceDescriptor descriptor);
}
