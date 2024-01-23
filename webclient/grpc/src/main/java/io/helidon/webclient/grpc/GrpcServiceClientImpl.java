package io.helidon.webclient.grpc;

import io.helidon.webclient.http2.Http2Client;

import io.grpc.ClientCall;
import io.grpc.MethodDescriptor;
import io.grpc.stub.ClientCalls;

class GrpcServiceClientImpl implements GrpcServiceClient {
    private final GrpcServiceDescriptor descriptor;
    private final Http2Client http2Client;

    GrpcServiceClientImpl(GrpcServiceDescriptor descriptor, Http2Client http2Client) {
        this.descriptor = descriptor;
        this.http2Client = http2Client;
    }

    @Override
    public <ReqT, RespT> RespT unary(String methodName, ReqT request) {
        ClientCall<? super ReqT, ? extends RespT> call = ensureMethod(methodName, MethodDescriptor.MethodType.UNARY);
        return ClientCalls.blockingUnaryCall(call, request);
    }

    private <ReqT, RespT> ClientCall<ReqT, RespT> ensureMethod(String methodName, MethodDescriptor.MethodType methodType) {
        ClientMethodDescriptor method = descriptor.method(methodName);

        if (!method.type().equals(methodType)) {
            throw new IllegalArgumentException("Method " + methodName + " is of type " + method.type() + ", yet " + methodType + " was requested.");
        }

        return createClientCall(method);
    }

    private <ReqT, RespT> ClientCall<ReqT, RespT> createClientCall(ClientMethodDescriptor method) {

        return new GrpcClientCall<>(http2Client, method);
    }
}
