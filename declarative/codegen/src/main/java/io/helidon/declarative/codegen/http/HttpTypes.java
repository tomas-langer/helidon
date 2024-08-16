package io.helidon.declarative.codegen.http;

import io.helidon.common.types.TypeName;

public class HttpTypes {
    public static final TypeName HTTP_METHOD = TypeName.create("io.helidon.http.Method");
    public static final TypeName HTTP_STATUS = TypeName.create("io.helidon.http.Status");
    public static final TypeName HTTP_HEADER_NAME = TypeName.create("io.helidon.http.HeaderName");
    public static final TypeName HTTP_HEADER_NAMES = TypeName.create("io.helidon.http.HeaderNames");
    public static final TypeName HTTP_HEADER = TypeName.create("io.helidon.http.Header");
    public static final TypeName HTTP_HEADER_VALUES = TypeName.create("io.helidon.http.HeaderValues");
    public static final TypeName HTTP_PATH_ANNOTATION = TypeName.create("io.helidon.http.Http.Path");
    public static final TypeName HTTP_METHOD_ANNOTATION = TypeName.create("io.helidon.http.Http.HttpMethod");
    public static final TypeName HTTP_PRODUCES_ANNOTATION = TypeName.create("io.helidon.http.Http.Produces");
    public static final TypeName HTTP_CONSUMES_ANNOTATION = TypeName.create("io.helidon.http.Http.Consumes");
    public static final TypeName HTTP_PATH_PARAM_ANNOTATION = TypeName.create("io.helidon.http.Http.PathParam");
    public static final TypeName HTTP_QUERY_PARAM_ANNOTATION = TypeName.create("io.helidon.http.Http.QueryParam");
    public static final TypeName HTTP_HEADER_PARAM_ANNOTATION = TypeName.create("io.helidon.http.Http.HeaderParam");
    public static final TypeName HTTP_ENTITY_PARAM_ANNOTATION = TypeName.create("io.helidon.http.Http.Entity");
    public static final TypeName HTTP_MEDIA_TYPE = TypeName.create("io.helidon.http.HttpMediaType");
}
