package io.helidon.webclient.grpc;

import java.util.Collection;

import io.grpc.stub.StreamObserver;

/**
 * Client for a single service.
 *
 * @see io.helidon.webclient.grpc.GrpcClient#serviceClient(io.helidon.webclient.grpc.GrpcServiceDescriptor)
 */
public interface GrpcServiceClient {
    /**
     * Name of the service this client was created for.
     *
     * @return service name
     */
    String serviceName();

    <ReqT, RespT> RespT unary(String methodName, ReqT request);

    <ReqT, RespT> StreamObserver<ReqT> unary(String methodName, StreamObserver<RespT> responseObserver);

    <ReqT, RespT> Collection<RespT> serverStream(String methodName, ReqT request);

    <ReqT, RespT> void serverStream(String methodName, ReqT request, StreamObserver<RespT> responseObserver);

    <ReqT, RespT> RespT clientStream(String methodName, Collection<ReqT> request);

    <ReqT, RespT> StreamObserver<ReqT> clientStream(String methodName, StreamObserver<RespT> responseObserver);

    <ReqT, RespT> Collection<RespT> bidi(String methodName, Collection<ReqT> responseObserver);

    <ReqT, RespT> StreamObserver<ReqT> bidi(String methodName, StreamObserver<RespT> responseObserver);
}
