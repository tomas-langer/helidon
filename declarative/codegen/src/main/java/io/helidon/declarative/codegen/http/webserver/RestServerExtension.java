/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.declarative.codegen.http.webserver;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.helidon.codegen.CodegenException;
import io.helidon.codegen.CodegenUtil;
import io.helidon.codegen.ElementInfoPredicates;
import io.helidon.codegen.TypeHierarchy;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.codegen.classmodel.Constructor;
import io.helidon.codegen.classmodel.Method;
import io.helidon.common.HelidonServiceLoader;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.declarative.codegen.FieldNames;
import io.helidon.declarative.codegen.http.RestExtensionBase;
import io.helidon.declarative.codegen.http.model.ComputedHeader;
import io.helidon.declarative.codegen.http.model.HeaderValue;
import io.helidon.declarative.codegen.http.model.HttpMethod;
import io.helidon.declarative.codegen.http.model.HttpStatus;
import io.helidon.declarative.codegen.http.model.RestMethod;
import io.helidon.declarative.codegen.http.model.RestMethodParameter;
import io.helidon.declarative.codegen.http.model.ServerEndpoint;
import io.helidon.declarative.codegen.http.webserver.spi.HttpParameterCodegenProvider;
import io.helidon.service.codegen.RegistryCodegenContext;
import io.helidon.service.codegen.RegistryRoundContext;
import io.helidon.service.codegen.ServiceCodegenTypes;
import io.helidon.service.codegen.spi.RegistryCodegenExtension;

import static io.helidon.codegen.CodegenUtil.toConstantName;
import static io.helidon.declarative.codegen.DeclarativeTypes.SINGLETON_ANNOTATION;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_ENTITY_PARAM_ANNOTATION;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_HEADER_PARAM_ANNOTATION;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_METHOD;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_METHOD_ANNOTATION;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_PATH_PARAM_ANNOTATION;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_QUERY_PARAM_ANNOTATION;
import static io.helidon.declarative.codegen.http.HttpTypes.HTTP_STATUS;
import static io.helidon.declarative.codegen.http.webserver.WebServerCodegenTypes.REST_SERVER_COMPUTED_HEADER;
import static io.helidon.declarative.codegen.http.webserver.WebServerCodegenTypes.REST_SERVER_COMPUTED_HEADERS;
import static io.helidon.declarative.codegen.http.webserver.WebServerCodegenTypes.REST_SERVER_ENDPOINT;
import static io.helidon.declarative.codegen.http.webserver.WebServerCodegenTypes.REST_SERVER_HEADER;
import static io.helidon.declarative.codegen.http.webserver.WebServerCodegenTypes.REST_SERVER_HEADERS;
import static io.helidon.declarative.codegen.http.webserver.WebServerCodegenTypes.REST_SERVER_LISTENER;
import static io.helidon.declarative.codegen.http.webserver.WebServerCodegenTypes.REST_SERVER_STATUS;
import static java.util.function.Predicate.not;

/*
Generates:
- Endpoint__HttpFeature.java
 */
class RestServerExtension extends RestExtensionBase implements RegistryCodegenExtension {
    private static final TypeName GENERATOR = TypeName.create(RestServerExtension.class);
    private static final String REQUEST_PARAM_NAME = "helidonDeclarative__server_req";
    private static final String RESPONSE_PARAM_NAME = "helidonDeclarative__server_res";
    private static final String METHOD_RESPONSE_NAME = "helidonDeclarative__response";
    private static final List<HttpParameterCodegenProvider> PARAM_PROVIDERS =
            HelidonServiceLoader.builder(ServiceLoader.load(HttpParameterCodegenProvider.class))
                    .addService(new ParamProviderHttpEntity())
                    .addService(new ParamProviderHttpHeader())
                    .addService(new ParamProviderHttpPathParam())
                    .addService(new ParamProviderHttpQuery())
                    .addService(new ParamProviderHttpReqRes())
                    .addService(new ParamProviderSecurityContext())
                    .addService(new ParamProviderContext())
                    .build()
                    .asList();

    private final RegistryCodegenContext ctx;

    RestServerExtension(RegistryCodegenContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void process(RegistryRoundContext roundContext) {
        // for each @RestServer.Endpoint generate a service that implements it
        Collection<TypeInfo> clientApis = roundContext.annotatedTypes(REST_SERVER_ENDPOINT);

        List<ServerEndpoint> endpoints = clientApis.stream()
                .map(this::toEndpoint)
                .collect(Collectors.toUnmodifiableList());

        for (ServerEndpoint endpoint : endpoints) {
            process(endpoint);
        }
    }

    private static void addSetupMethod(ClassModel.Builder endpointService, String path) {
        endpointService.addMethod(setup -> setup
                .accessModifier(AccessModifier.PUBLIC)
                .addAnnotation(Annotations.OVERRIDE)
                .name("setup")
                .addParameter(routing -> routing
                        .name("routing")
                        .type(WebServerCodegenTypes.SERVER_HTTP_ROUTING_BUILDER))
                .addContent("routing.register(\"")
                .addContent(path)
                .addContentLine("\", this::routing);"));
    }

    private void constructor(Constructor.Builder constructor,
                             TypeName endpoint,
                             boolean singleton,
                             FieldNames<TypeName> headerProducers) {
        constructor.accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .addAnnotation(Annotation.create(ServiceCodegenTypes.INJECTION_INJECT))
                .addParameter(param -> param
                        .type(singleton ? endpoint : supplierOf(endpoint))
                        .name("endpoint"))
                .addContentLine("this.endpoint = endpoint;")
                .update(builder -> {
                    headerProducers.forEach((producerType, producerField) -> {
                        builder.addParameter(producer -> producer
                                .type(producerType)
                                .name(producerField));
                        builder.addContent("this.")
                                .addContent(producerField)
                                .addContent(" = ")
                                .addContent(producerField)
                                .addContentLine(";");
                    });
                });
    }

    private void methodHandlers(ClassModel.Builder classModel,
                                Constructor.Builder constructor,
                                TypeName descriptorType,
                                List<RestMethod> methods) {
        // create constants with method metadata
        // create fields for each handler
        // create handlers in constructor

        constructor.addParameter(httpEntryPoints -> httpEntryPoints
                .type(WebServerCodegenTypes.DECLARATIVE_ENTRY_POINTS)
                .name("entryPoints")
        );

        constructor
                .addContent("var descriptor = ")
                .addContent(descriptorType)
                .addContentLine(".INSTANCE;");
        constructor
                .addContent("var annotations = ")
                .addContent(descriptorType)
                .addContentLine(".ANNOTATIONS;")
                .addContentLine("");

        for (RestMethod method : methods) {
            String uniqueName = method.uniqueName();
            String constant = toConstantName("METHOD_" + uniqueName);
            String field = "handler_" + uniqueName;

            classModel.addField(handlerField -> handlerField
                    .accessModifier(AccessModifier.PRIVATE)
                    .isFinal(true)
                    .type(WebServerCodegenTypes.SERVER_HTTP_HANDLER)
                    .name(field)
            );

            classModel.addField(methodMeta -> methodMeta
                    .update(this::privateConstant)
                    .type(TypedElementInfo.class)
                    .name(constant)
                    .addContentCreate(method.method())
            );

            constructor.addContent("this.")
                    .addContent(field)
                    .addContentLine(" = entryPoints.handler(")
                    .increaseContentPadding()
                    .increaseContentPadding()
                    .addContentLine("descriptor,")
                    .addContentLine("descriptor.qualifiers(),")
                    .addContentLine("annotations,")
                    .addContent(constant)
                    .addContentLine(",")
                    .addContent("this::")
                    .addContent(method.uniqueName())
                    .addContentLine(");")
                    .decreaseContentPadding()
                    .decreaseContentPadding();
        }
    }

    private void addFields(ClassModel.Builder endpointService, TypeName endpointType, boolean singleton) {
        endpointService.addField(endpointField -> endpointField
                .accessModifier(AccessModifier.PRIVATE)
                .isFinal(true)
                .type(singleton ? endpointType : supplierOf(endpointType))
                .name("endpoint")
        );
    }

    private ServerEndpoint toEndpoint(TypeInfo typeInfo) {
        var builder = ServerEndpoint.builder()
                .type(typeInfo);

        List<Annotation> typeAnnotations = TypeHierarchy.hierarchyAnnotations(ctx, typeInfo);
        builder.annotations(typeAnnotations);

        Annotations.findFirst(REST_SERVER_LISTENER, typeAnnotations)
                .ifPresent(listener -> {
                    listener.stringValue().ifPresent(builder::listener);
                    listener.booleanValue("required").ifPresent(builder::listenerRequired);
                });

        path(typeAnnotations, builder);
        produces(typeAnnotations, builder);
        consumes(typeAnnotations, builder);
        headers(typeAnnotations, builder, REST_SERVER_HEADERS, REST_SERVER_HEADER);
        computedHeaders(typeAnnotations, builder, REST_SERVER_COMPUTED_HEADERS, REST_SERVER_COMPUTED_HEADER);

        // methods - each class method annotated with HTTP Method meta-annotation is a valid one
        Map<String, AtomicInteger> uniqueMethodNames = new HashMap<>();

        typeInfo.elementInfo()
                .stream()
                .filter(ElementInfoPredicates::isMethod)
                .filter(not(ElementInfoPredicates::isPrivate))
                .filter(not(ElementInfoPredicates::isStatic))
                .forEach(it -> toMethod(typeInfo, builder, uniqueMethodNames, it));

        return builder.build();
    }

    private void toMethod(TypeInfo endpoint,
                          ServerEndpoint.Builder endpointBuilder,
                          Map<String, AtomicInteger> uniqueMethodNames,
                          TypedElementInfo method) {
        List<Annotation> annotations = TypeHierarchy.hierarchyAnnotations(ctx, endpoint, method);

        Optional<Annotation> httpMethodAnnotation = Annotations.findFirst(HTTP_METHOD_ANNOTATION, annotations);
        if (httpMethodAnnotation.isEmpty()) {
            // this method does not have an Http.Method meta annotation present, we can skip it
            return;
        }

        String methodName = method.elementName();
        int counter = uniqueMethodNames.computeIfAbsent(methodName,
                                                        it -> new AtomicInteger())
                .getAndIncrement();
        String uniqueName = counter == 0
                ? methodName
                : methodName + "_" + counter;

        var builder = RestMethod.builder()
                .returnType(method.typeName())
                .type(endpoint)
                .name(methodName)
                .uniqueName(uniqueName)
                .method(method)
                .annotations(annotations)
                .httpMethod(httpMethodFromAnnotation(method, httpMethodAnnotation.get()));

        path(annotations, builder);
        consumes(annotations, builder);
        produces(annotations, builder);
        headers(annotations, builder, REST_SERVER_HEADERS, REST_SERVER_HEADER);
        computedHeaders(annotations, builder, REST_SERVER_COMPUTED_HEADERS, REST_SERVER_COMPUTED_HEADER);

        if (builder.consumes().isEmpty()) {
            builder.consumes(endpointBuilder.consumes());
        }
        if (builder.produces().isEmpty()) {
            builder.produces(endpointBuilder.produces());
        }
        builder.addHeaders(endpointBuilder.headers());
        builder.addComputedHeaders(endpointBuilder.computedHeaders());

        Annotations.findFirst(REST_SERVER_STATUS, annotations)
                .ifPresent(annotation -> {
                    int code = annotation.intValue().orElse(200);
                    Optional<String> reason = annotation
                            .stringValue("reason")
                            .filter(not(String::isBlank));
                    builder.status(new HttpStatus(code, reason));
                });

        int index = 0;
        for (TypedElementInfo parameterInfo : method.parameterArguments()) {
            processEndpointParameter(endpoint, method, parameterInfo, builder, index);
            index++;
        }

        endpointBuilder.addMethod(builder.build());
    }

    private void processEndpointParameter(TypeInfo typeInfo,
                                          TypedElementInfo methodInfo,
                                          TypedElementInfo parameterInfo,
                                          RestMethod.Builder method,
                                          int index) {
        List<Annotation> annotations = TypeHierarchy.hierarchyAnnotations(ctx, typeInfo, methodInfo, parameterInfo, index);
        var parameter = RestMethodParameter.builder()
                .annotations(annotations)
                .name(parameterInfo.elementName())
                .typeName(parameterInfo.typeName())
                .index(index)
                .method(methodInfo)
                .type(typeInfo)
                .parameter(parameterInfo)
                .build();

        method.addParameter(parameter);
        if (Annotations.findFirst(HTTP_HEADER_PARAM_ANNOTATION, annotations).isPresent()) {
            method.addHeaderParameter(parameter);
        }
        if (Annotations.findFirst(HTTP_QUERY_PARAM_ANNOTATION, annotations).isPresent()) {
            method.addQueryParameter(parameter);
        }
        if (Annotations.findFirst(HTTP_PATH_PARAM_ANNOTATION, annotations).isPresent()) {
            method.addPathParameter(parameter);
        }
        if (Annotations.findFirst(HTTP_ENTITY_PARAM_ANNOTATION, annotations).isPresent()) {
            method.entityParameter(parameter);
        }
    }

    private void process(ServerEndpoint endpoint) {
        TypeInfo type = endpoint.type();
        if (type.kind() == ElementKind.INTERFACE) {
            // interfaces are ignored, we must have an implementation
            return;
        }
        TypeName endpointTypeName = type.typeName();

        var headerNameConstants = FieldNames.<String>create("HEADER_NAME_");
        var headerValueConstants = FieldNames.<HeaderValue>create("HEADER_");
        var mediaTypeConstants = FieldNames.<String>create("MEDIA_TYPE_");
        var httpMethodConstants = FieldNames.<String>create("METHOD_");
        var httpStatusConstants = FieldNames.<HttpStatus>create("STATUS_");
        var headerProducers = FieldNames.<TypeName>create("headerProducer_");

        headerValueConstants.addAll(endpoint.headers());
        addComputedHeaderConstants(endpoint.computedHeaders(), headerNameConstants);

        // now for each method
        for (RestMethod method : endpoint.methods()) {
            headerValueConstants.addAll(method.headers());
            method.computedHeaders()
                    .stream()
                    .map(ComputedHeader::name)
                    .forEach(headerNameConstants::add);
            mediaTypeConstants.addAll(method.produces());
            mediaTypeConstants.addAll(method.consumes());
            addMethodConstant(method.httpMethod(), httpMethodConstants);
            method.status().ifPresent(httpStatusConstants::add);
            method.computedHeaders()
                    .stream()
                    .map(ComputedHeader::producer)
                    .forEach(headerProducers::add);

            // parameters
            for (RestMethodParameter parameter : method.headerParameters()) {
                addHeaderNameConstants(TypeHierarchy.hierarchyAnnotations(ctx,
                                                                          parameter.type(),
                                                                          parameter.method(),
                                                                          parameter.parameter(),
                                                                          parameter.index()),
                                       headerNameConstants);
            }
        }

        // we have all the necessary constants, time to build the implementation
        String classNameBase = endpointTypeName.classNameWithEnclosingNames().replace('.', '_');
        String className = classNameBase + "__HttpFeature";
        TypeName generatedType = TypeName.builder()
                .packageName(endpointTypeName.packageName())
                .className(className)
                .build();

        ClassModel.Builder classModel = ClassModel.builder()
                .copyright(CodegenUtil.copyright(GENERATOR,
                                                 endpointTypeName,
                                                 generatedType))
                .addAnnotation(CodegenUtil.generatedAnnotation(GENERATOR,
                                                               endpointTypeName,
                                                               generatedType,
                                                               "1",
                                                               ""))
                .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .type(generatedType)
                .addAnnotation(SINGLETON_ANNOTATION)
                .addInterface(WebServerCodegenTypes.SERVER_HTTP_FEATURE);

        boolean singleton = type.hasAnnotation(ServiceCodegenTypes.INJECTION_SINGLETON);
        // adds the endpoint field (may be a supplier)
        addFields(classModel, endpointTypeName, singleton);
        headerProducers.forEach((producerType, producerField) -> {
            classModel.addField(producer -> producer
                    .accessModifier(AccessModifier.PRIVATE)
                    .isFinal(true)
                    .type(producerType)
                    .name(producerField));
        });

        // constructor injecting the field(s)
        var constructor = Constructor.builder();
        constructor(constructor, endpointTypeName, singleton, headerProducers);
        methodHandlers(classModel, constructor, ctx.descriptorType(type.typeName()), endpoint.methods());
        classModel.addConstructor(constructor);
        // HttpFeature.setup(HttpRouting.Builder routing)
        addSetupMethod(classModel, endpoint.path().orElse("/"));
        // socket() and socketRequired()
        addSocketMethods(classModel, endpoint);
        // private void routing(HttpRules rules)
        addRoutingMethod(classModel,
                         endpoint,
                         httpMethodConstants,
                         mediaTypeConstants);

        int methodIndex = 0;
        for (RestMethod restMethod : endpoint.methods()) {
            addEndpointMethod(endpointTypeName,
                              classModel,
                              singleton,
                              mediaTypeConstants,
                              headerNameConstants,
                              headerValueConstants,
                              httpStatusConstants,
                              restMethod,
                              headerProducers,
                              methodIndex);
            methodIndex++;
        }

        // create constants (after all parameter providers are handled)
        mediaTypeConstants(classModel, mediaTypeConstants);
        headerNameConstants(classModel, headerNameConstants);
        headerValueConstants(classModel, headerValueConstants);
        httpMethodConstants(classModel, httpMethodConstants);
        statusConstants(classModel, httpStatusConstants);

        ctx.addType(generatedType, classModel, endpointTypeName, type.originatingElement().orElse(endpointTypeName));
    }

    private void addSocketMethods(ClassModel.Builder classModel, ServerEndpoint endpoint) {
        Optional<String> listener = endpoint.listener();
        if (listener.isEmpty()) {
            return;
        }
        classModel.addMethod(socket -> socket
                .accessModifier(AccessModifier.PUBLIC)
                .returnType(TypeNames.STRING)
                .name("socket")
                .addAnnotation(Annotations.OVERRIDE)
                .addContent("return \"")
                .addContent(listener.get())
                .addContentLine("\";"));

        if (endpoint.listenerRequired()) {
            classModel.addMethod(socket -> socket
                    .accessModifier(AccessModifier.PUBLIC)
                    .returnType(TypeNames.PRIMITIVE_BOOLEAN)
                    .name("socketRequired")
                    .addAnnotation(Annotations.OVERRIDE)
                    .addContentLine("return true;"));
        }
    }

    @SuppressWarnings("checkstyle:ParameterNumber") // this is a private method
    private void addEndpointMethod(TypeName endpointTypeName,
                                   ClassModel.Builder classModel,
                                   boolean singleton,
                                   FieldNames<String> mediaTypeConstants,
                                   FieldNames<String> headerNameConstants,
                                   FieldNames<HeaderValue> headerValueConstants,
                                   FieldNames<HttpStatus> httpStatusConstants,
                                   RestMethod restMethod,
                                   FieldNames<TypeName> headerProducers,
                                   int methodIndex) {

        classModel.addMethod(method -> method
                .accessModifier(AccessModifier.PRIVATE)
                .name(restMethod.uniqueName())
                .addParameter(req -> req
                        .type(WebServerCodegenTypes.SERVER_REQUEST)
                        .name(REQUEST_PARAM_NAME))
                .addParameter(res -> res
                        .type(WebServerCodegenTypes.SERVER_RESPONSE)
                        .name(RESPONSE_PARAM_NAME))
                .update(it -> restMethod.method().throwsChecked()
                        .forEach(checked -> it.addThrows(thrown -> thrown.type(checked))))
                .update(it -> endpointMethodBody(endpointTypeName,
                                                 classModel,
                                                 singleton,
                                                 mediaTypeConstants,
                                                 headerNameConstants,
                                                 headerValueConstants,
                                                 httpStatusConstants,
                                                 it,
                                                 restMethod,
                                                 headerProducers,
                                                 methodIndex)));
    }

    @SuppressWarnings("checkstyle:ParameterNumber") // this is a private method
    private void endpointMethodBody(TypeName endpointType,
                                    ClassModel.Builder classModel,
                                    boolean singleton,
                                    FieldNames<String> mediaTypeConstants,
                                    FieldNames<String> headerNameConstants,
                                    FieldNames<HeaderValue> headerValueConstants,
                                    FieldNames<HttpStatus> httpStatusConstants,
                                    Method.Builder method,
                                    RestMethod restMethod,
                                    FieldNames<TypeName> headerProducers,
                                    int methodIndex) {
        // parameters
        for (RestMethodParameter parameter : restMethod.parameters()) {
            String paramName = parameter.name();
            method.addContent("var ")
                    .addContent(paramName)
                    .addContent(" = ");
            invokeParamHandler(endpointType,
                               classModel,
                               method,
                               restMethod,
                               parameter,
                               methodIndex,
                               mediaTypeConstants,
                               headerNameConstants,
                               headerValueConstants);
            method.addContentLine("");
        }

        boolean hasResponse = false;
        if (!restMethod.returnType().boxed().equals(TypeNames.BOXED_VOID)) {
            method.addContent("var ")
                    .addContent(METHOD_RESPONSE_NAME)
                    .addContent(" = ");
            hasResponse = true;
        }

        List<RestMethodParameter> params = restMethod.parameters();
        if (singleton) {
            method.addContent("this.endpoint.");
        } else {
            method.addContent("this.endpoint.get().");
        }
        method.addContent(restMethod.name())
                .addContent("(");
        if (params.isEmpty()) {
            method.addContentLine(");");
        } else if (params.size() == 1) {
            method.addContent(params.getFirst().name())
                    .addContentLine(");");
        } else {
            // more than one parameter, multiline
            method.addContentLine("")
                    .increaseContentPadding()
                    .increaseContentPadding();
            Iterator<RestMethodParameter> iterator = params.iterator();
            while (iterator.hasNext()) {
                RestMethodParameter next = iterator.next();
                method.addContent(next.name());
                if (iterator.hasNext()) {
                    method.addContentLine(",");
                }
            }
            method.addContentLine(");")
                    .decreaseContentPadding()
                    .decreaseContentPadding();
        }

        if (restMethod.produces().size() == 1) {
            String mediaType = restMethod.produces().getFirst();
            if (!"*/*".equals(mediaType)) {
                method.addContent(RESPONSE_PARAM_NAME)
                        .addContent(".headers().contentType(")
                        .addContent(mediaTypeConstants.get(mediaType))
                        .addContentLine(");");
            }
        }

        if (restMethod.status().isPresent()) {
            HttpStatus httpStatus = restMethod.status().get();

            method.addContent(RESPONSE_PARAM_NAME)
                    .addContent(".status(")
                    .addContent(httpStatusConstants.get(httpStatus))
                    .addContentLine(");");
        }

        // now each header value, header producer, and header parameter
        for (HeaderValue header : restMethod.headers()) {
            method.addContent("helidonDeclarative__server_res.header(")
                    .addContent(headerValueConstants.get(header))
                    .addContentLine(");");
        }
        for (ComputedHeader computedHeader : restMethod.computedHeaders()) {
            String headerNameConstant = headerNameConstants.get(computedHeader.name());

            method.addContent(headerProducers.get(computedHeader.producer()))
                    .addContent(".produceHeader(")
                    .addContent(headerNameConstant)
                    .addContent(").ifPresent(declarative__it -> helidonDeclarative__server_res.header(")
                    .addContent(headerNameConstant)
                    .addContentLine(", declarative__it));");
        }

        method.addContent(RESPONSE_PARAM_NAME)
                .addContent(".send(");
        if (hasResponse) {
            // we consider the response to be an entity to be sent (unmodified) over the response
            method.addContent(METHOD_RESPONSE_NAME);
        }
        method.addContentLine(");");
    }

    @SuppressWarnings("checkstyle:ParameterNumber") // this is a private method
    private void invokeParamHandler(TypeName endpointType,
                                    ClassModel.Builder classModel,
                                    Method.Builder method,
                                    RestMethod restMethod,
                                    RestMethodParameter param,
                                    int methodIndex,
                                    FieldNames<String> mediaTypeConstants,
                                    FieldNames<String> headerNameConstants,
                                    FieldNames<HeaderValue> headerValueConstants) {
        for (HttpParameterCodegenProvider paramProvider : PARAM_PROVIDERS) {
            try {
                if (paramProvider.codegen(new ParamCodegenContextImpl(param.annotations(),
                                                                      param.typeName(),
                                                                      classModel,
                                                                      method,
                                                                      REQUEST_PARAM_NAME,
                                                                      RESPONSE_PARAM_NAME,
                                                                      endpointType,
                                                                      restMethod.name(),
                                                                      param.name(),
                                                                      methodIndex,
                                                                      param.index(),
                                                                      mediaTypeConstants,
                                                                      headerNameConstants,
                                                                      headerValueConstants))) {
                    return;
                }
            } catch (Exception e) {
                throw new CodegenException("Failed to process parameter '" + param.typeName().resolvedName() + " "
                                                   + param.name() + "' that is " + (param.index() + 1) + " parameter of method "
                                                   + endpointType.fqName() + "." + restMethod.name()
                                                   + ", as the parameter handler ("
                                                   + paramProvider.getClass().getName() + ") threw an exception.",
                                           e);
            }
        }
        throw new CodegenException("Failed to process parameter '" + param.typeName().resolvedName() + " "
                                           + param.name() + "' that is " + (param.index() + 1) + " parameter of method "
                                           + endpointType.fqName() + "." + restMethod.name()
                                           + ", as there is no parameter handler registered for it.");
    }

    private void addRoutingMethod(ClassModel.Builder classModel,
                                  ServerEndpoint endpoint,
                                  FieldNames<String> httpMethodConstants,
                                  FieldNames<String> mediaTypeConstants) {

        classModel.addMethod(routing -> routing
                .accessModifier(AccessModifier.PRIVATE)
                .name("routing")
                .addParameter(rules -> rules
                        .type(WebServerCodegenTypes.SERVER_HTTP_RULES)
                        .name("rules"))
                .update(it -> routingMethodBody(it,
                                                endpoint,
                                                httpMethodConstants,
                                                mediaTypeConstants)));
    }

    private void routingMethodBody(Method.Builder method,
                                   ServerEndpoint endpoint,
                                   FieldNames<String> httpMethodConstants,
                                   FieldNames<String> mediaTypeConstants) {

        for (RestMethod restMethod : endpoint.methods()) {
            if (restMethod.produces().isEmpty() && restMethod.consumes().isEmpty()) {
                addSimpleRoute(method, restMethod, httpMethodConstants);
            } else {
                addHttpRoute(method, restMethod, httpMethodConstants, mediaTypeConstants);
            }
        }
    }

    private void addSimpleRoute(Method.Builder routing, RestMethod restMethod, FieldNames<String> httpMethodConstants) {
        routing.addContent("rules.");

        HttpMethod httpMethod = restMethod.httpMethod();
        if (httpMethod.builtIn()) {
            routing.addContent(httpMethod.name().toLowerCase(Locale.ROOT))
                    .addContent("(");
        } else {
            routing.addContent("route(" + httpMethodConstants.get(httpMethod.name()) + ", ");
        }

        String path = restMethod.path().orElse("/");

        routing.addContent("\"")
                .addContent(path)
                .addContent("\", ");

        routing.addContent("handler_")
                .addContent(restMethod.uniqueName())
                .addContentLine(");");
    }

    private void addHttpRoute(Method.Builder routing,
                              RestMethod restMethod,
                              FieldNames<String> httpMethodConstants,
                              FieldNames<String> mediaTypeConstants) {
        routing.addContent("rules.route(")
                .addContent(WebServerCodegenTypes.SERVER_HTTP_ROUTE)
                .addContentLine(".builder()")
                .increaseContentPadding()
                .increaseContentPadding()
                .addContent(".methods(");

        HttpMethod httpMethod = restMethod.httpMethod();
        if (httpMethod.builtIn()) {
            routing.addContent(HTTP_METHOD)
                    .addContent(".")
                    .addContent(httpMethod.name());
        } else {
            routing.addContent(httpMethodConstants.get(httpMethod.name()));
        }

        routing.addContentLine(")"); // end of methods(Method.GET)

        boolean consumesExists = !restMethod.consumes().isEmpty();

        routing.addContent(".headers(headers -> ");

        if (consumesExists) {
            routing.addContent("headers.testContentType(");
            routing.addContent(restMethod.consumes()
                                       .stream()
                                       .map(mediaTypeConstants::get)
                                       .collect(Collectors.joining(", ")));
            routing.addContent(")");
        }

        if (!restMethod.produces().isEmpty()) {
            if (consumesExists) {
                routing.addContent(" && ");
            }
            routing.addContent("headers.isAccepted(");
            routing.addContent(restMethod.produces()
                                       .stream()
                                       .map(mediaTypeConstants::get)
                                       .collect(Collectors.joining(", ")));
            routing.addContent(")");
        }

        routing.addContentLine(")");

        String path = restMethod.path().orElse("/");
        routing.addContent(".path(\"")
                .addContent(path)
                .addContentLine("\")")
                .addContent(".handler(handler_")
                .addContent(restMethod.uniqueName())
                .addContentLine(")");

        routing.addContentLine(".build());")
                .decreaseContentPadding()
                .decreaseContentPadding();
    }

    private void statusConstants(ClassModel.Builder classModel, FieldNames<HttpStatus> constants) {
        constants.forEach((status, constant) -> {
            classModel.addField(headerValue -> headerValue
                    .update(this::privateConstant)
                    .type(HTTP_STATUS)
                    .name(constant)
                    .addContent(HTTP_STATUS)
                    .addContent(".create(")
                    .addContent(String.valueOf(status.code()))
                    .update(it -> {
                        if (status.reason().isPresent()) {
                            it.addContent(", \"")
                                    .addContent(status.reason().get())
                                    .addContent("\"");
                        }
                    })
                    .addContent(")"));
        });
    }
}
