package io.helidon.webclient.grpc;

import io.helidon.common.socket.SocketContext;
import io.helidon.http.http2.Http2Settings;
import io.helidon.http.http2.Http2StreamState;
import io.helidon.http.http2.StreamFlowControl;
import io.helidon.webclient.http2.Http2ClientConfig;

class GrpcClientProtocol {
    static GrpcClientStream create(SocketContext scoketContext,
                                   Http2Settings serverSettings,
                                   Http2ClientConfig clientConfig,
                                   int streamId,
                                   StreamFlowControl flowControl,
                                   Http2StreamState streamState) {

    }
}
