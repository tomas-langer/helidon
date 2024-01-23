package io.helidon.webclient.grpc;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import io.grpc.CallCredentials;
import io.grpc.ClientInterceptor;

/**
 * All required meta-data about a client side gRPC service.
 */
public class GrpcServiceDescriptor {
    private final String serviceName;
    private final Map<String, ClientMethodDescriptor> methods;
    private final List<ClientInterceptor> interceptors;
    private CallCredentials callCredentials;

    ClientMethodDescriptor method(String name) {
        ClientMethodDescriptor clientMethodDescriptor = methods.get(name);
        if (clientMethodDescriptor == null) {
            throw new NoSuchElementException("There is no method " + name + " defined for service " + this);
        }
        return clientMethodDescriptor;
    }
}
