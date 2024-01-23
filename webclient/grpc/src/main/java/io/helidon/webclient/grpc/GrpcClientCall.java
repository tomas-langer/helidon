package io.helidon.webclient.grpc;

import java.util.concurrent.atomic.AtomicReference;

import io.helidon.http.Header;
import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;
import io.helidon.webclient.http2.Http2Client;

import io.grpc.ClientCall;
import io.grpc.Metadata;

class GrpcClientCall<ReqT, ResT> extends ClientCall<ReqT, ResT> {
    private static final Header GRPC_CONTENT_TYPE = HeaderValues.create(HeaderNames.CONTENT_TYPE, "application/grpc");

    private final AtomicReference<Listener<ResT>> responseListener = new AtomicReference<>();
    private final Http2Client http2Client;
    private final ClientMethodDescriptor method;

    GrpcClientCall(Http2Client http2Client, ClientMethodDescriptor method) {
        this.http2Client = http2Client;
        this.method = method;
    }

    @Override
    public void start(Listener<ResT> responseListener, Metadata headers) {
        // connect
        // send headers
        if (this.responseListener.compareAndSet(null, responseListener)) {
            Http2ClientConnection http2ClientRequest = http2Client
                    .post("") // must be post
                    .header(GRPC_CONTENT_TYPE)
                    .connect();

        } else {
            throw new IllegalStateException("Response listener was already set");
        }
    }

    @Override
    public void request(int numMessages) {

    }

    @Override
    public void cancel(String message, Throwable cause) {

    }

    @Override
    public void halfClose() {

    }

    @Override
    public void sendMessage(ReqT message) {

    }
}
