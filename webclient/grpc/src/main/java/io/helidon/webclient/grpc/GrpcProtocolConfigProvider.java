package io.helidon.webclient.grpc;

import io.helidon.common.config.Config;
import io.helidon.webclient.spi.ProtocolConfigProvider;

/**
 * Implementation of protocol config provider for gRPC.
 */
public class GrpcProtocolConfigProvider implements ProtocolConfigProvider<GrpcClientProtocolConfig> {
    /**
     * Required to be used by {@link java.util.ServiceLoader}.
     * @deprecated do not use directly, use Http1ClientProtocol
     */
    public GrpcProtocolConfigProvider() {
    }

    @Override
    public String configKey() {
        return GrpcProtocolProvider.CONFIG_KEY;
    }

    @Override
    public GrpcClientProtocolConfig create(Config config, String name) {
        return GrpcClientProtocolConfig.builder()
                .config(config)
                .name(name)
                .build();
    }
}
