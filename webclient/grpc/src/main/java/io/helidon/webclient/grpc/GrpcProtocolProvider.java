package io.helidon.webclient.grpc;

import io.helidon.webclient.api.WebClient;
import io.helidon.webclient.http2.Http2Client;
import io.helidon.webclient.spi.ClientProtocolProvider;

public class GrpcProtocolProvider implements ClientProtocolProvider<GrpcClient, GrpcClientProtocolConfig> {
    static final String CONFIG_KEY = "grpc";

    @Override
    public String protocolId() {
        return Http2Client.PROTOCOL_ID;
    }

    @Override
    public Class<GrpcClientProtocolConfig> configType() {
        return GrpcClientProtocolConfig.class;
    }

    @Override
    public GrpcClientProtocolConfig defaultConfig() {
        return GrpcClientProtocolConfig.create();
    }

    @Override
    public GrpcClient protocol(WebClient client, GrpcClientProtocolConfig config) {
        return new GrpcClientImpl(client,
                                  GrpcClientConfig.builder()
                                          .from(client.prototype())
                                          .protocolConfig(config)
                                          .buildPrototype());
    }
}
