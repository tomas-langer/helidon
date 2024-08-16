package io.helidon.declarative.codegen.http.restclient;

import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeName;

final class RestClientTypes {
    static final TypeName REST_CLIENT_ENDPOINT = TypeName.create("io.helidon.webclient.api.RestClient.Endpoint");
    static final TypeName REST_CLIENT_QUALIFIER = TypeName.create("io.helidon.webclient.api.RestClient.Client");
    static final TypeName REST_CLIENT_HEADER = TypeName.create("io.helidon.webclient.api.RestClient.Header");
    static final TypeName REST_CLIENT_HEADERS = TypeName.create("io.helidon.webclient.api.RestClient.Headers");
    static final TypeName REST_CLIENT_COMPUTED_HEADER = TypeName.create("io.helidon.webclient.api.RestClient.ComputedHeader");
    static final TypeName REST_CLIENT_COMPUTED_HEADERS = TypeName.create("io.helidon.webclient.api.RestClient.ComputedHeaders");
    static final TypeName REST_CLIENT_HEADER_PRODUCER = TypeName.create("io.helidon.webclient.api.RestClient.HeaderProducer");
    static final TypeName REST_CLIENT_ERROR_HANDLER = TypeName.create("io.helidon.webclient.api.RestClient.ErrorHandler");
    static final TypeName REST_CLIENT_ERROR_HANDLING = TypeName.create("io.helidon.webclient.api.RestClient.ErrorHandling");
    static final TypeName WEB_CLIENT = TypeName.create("io.helidon.webclient.api.WebClient");

    static final Annotation REST_CLIENT_QUALIFIER_INSTANCE = Annotation.create(REST_CLIENT_QUALIFIER);
    private RestClientTypes() {
    }
}
