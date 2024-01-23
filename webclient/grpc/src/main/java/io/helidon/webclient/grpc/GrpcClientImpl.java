package io.helidon.webclient.grpc;

import io.helidon.webclient.api.WebClient;
import io.helidon.webclient.http2.Http2Client;

class GrpcClientImpl implements GrpcClient {
    private final WebClient webClient;
    private final Http2Client http2Client;
    private final GrpcClientConfig clientConfig;

    GrpcClientImpl(WebClient webClient, GrpcClientConfig clientConfig) {
        this.webClient = webClient;
        this.http2Client = webClient.client(Http2Client.PROTOCOL);
        this.clientConfig = clientConfig;
    }

    @Override
    public GrpcClientConfig prototype() {
        return clientConfig;
    }
}
