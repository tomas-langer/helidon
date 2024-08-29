package io.helidon.declarative.tests.http.security;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import io.helidon.common.Weight;
import io.helidon.common.config.Config;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.common.uri.UriInfo;
import io.helidon.declarative.webserver.HttpEntryPointInterceptor;
import io.helidon.http.Status;
import io.helidon.security.AuditEvent;
import io.helidon.security.EndpointConfig;
import io.helidon.security.Security;
import io.helidon.security.SecurityContext;
import io.helidon.security.SecurityEnvironment;
import io.helidon.security.SecurityLevel;
import io.helidon.security.annotations.Audited;
import io.helidon.security.annotations.Authenticated;
import io.helidon.security.annotations.Authorized;
import io.helidon.security.integration.common.SecurityTracing;
import io.helidon.security.internal.SecurityAuditEvent;
import io.helidon.security.providers.common.spi.AnnotationAnalyzer;
import io.helidon.service.inject.api.Injection;
import io.helidon.service.inject.api.InvocationContext;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

@Injection.Singleton
@Weight(800)
class HttpSecurityInterceptor implements HttpEntryPointInterceptor {
    private static final System.Logger LOGGER = System.getLogger(HttpSecurityInterceptor.class.getName());

    private final Security security;
    private final Config config;
    private final List<AnnotationAnalyzer> annotationAnalyzers;

    private final Map<TypeName, HttpSecurityDefinition> typeSecurityDefinitions = new HashMap<>();
    private final ReentrantReadWriteLock typeSecurityDefinitionLock = new ReentrantReadWriteLock();
    private final Map<Signature, HttpSecurityDefinition> methodSecurityDefinitions = new HashMap<>();
    private final ReentrantReadWriteLock methodSecurityDefinitionLock = new ReentrantReadWriteLock();

    HttpSecurityInterceptor(Security security,
                            Config config,
                            List<AnnotationAnalyzer> annotationAnalyzers) {
        this.security = security;
        this.config = config.get("server.features.security.declarative");
        this.annotationAnalyzers = annotationAnalyzers;
    }

    @Override
    public void proceed(InvocationContext ctx, Chain chain, ServerRequest req, ServerResponse res) throws Exception {
        HttpSecurityDefinition definition = methodSecurity(ctx);

        if (definition.noSecurity()) {
            chain.proceed(req, res);
            return;
        }

        /*
        Now whatever security was supposed to be handled by WebServer security is done
            (based on Security feature, usually from configuration)
        This code is responsible for
        - analyzing the annotations on the type and method
        - making sure that all was done, and if not, it does it here
        - we may want to code generate the security handler for each method, and just look it up based on name
            (such `as io.helidon.examples.declarative.GreetEndpoint.greet(java.lang.String)`)
        - for now, we create the handler in memory and cache it, and then invoke it
         */

        SecurityTracing tracing = SecurityTracing.get(req.context());
        UriInfo requestedUri = req.requestedUri();
        SecurityContext securityContext = req.context()
                .get(SecurityContext.class)
                .orElseThrow();

        String resourceType = ctx.serviceInfo().serviceType().fqName();
        var securityEnvironment = SecurityEnvironment.builder(security.serverTime())
                .transport(requestedUri.scheme())
                .path(requestedUri.path().path())
                .targetUri(requestedUri.toUri())
                .method(req.prologue().method().text())
                .queryParams(req.query())
                .headers(req.headers().toMap())
                .addAttribute("resourceType", resourceType)
                .addAttribute("userIp", req.remotePeer().host())
                .addAttribute("userPort", req.remotePeer().port())
                .build();

        var ec = EndpointConfig.builder()
                .securityLevels(definition.securityLevels())
                .build();

        var ictx = new HttpSecurityInterceptorContext();

        try {
            securityContext.env(securityEnvironment);
            securityContext.endpointConfig(ec);

            processSecurity(req, res, ictx, tracing, definition, securityContext);
        } finally {
            if (ictx.traceSuccess()) {
                tracing.logProceed();
                tracing.finish();
            } else {
                tracing.logDeny();
                tracing.error("aborted");
            }
        }

        if (ictx.shouldFinish()) {
            return;
        }

        chain.proceed(req, res);
        if (definition.atzExplicit() && !securityContext.isAuthorized()) {
            if (res.status().family() == Status.Family.CLIENT_ERROR
                    || res.status().family() == Status.Family.SERVER_ERROR) {
                // failure returned anyway - may have never reached the endpoint
                return;
            }
            if (res.isSent()) {
                LOGGER.log(System.Logger.Level.ERROR, "Authorization failure. Request for"
                        + req.prologue().uriPath().absolute().path()
                        + " has failed, it was marked for explicit authorization, "
                        + "yet authorization was never called on security context. The "
                        + "method was invoked and may have changed data, and it has sent a response."
                        + " Endpoint: " + ctx.serviceInfo().serviceType().fqName() + ", method: "
                        + ctx.elementInfo().elementName());
            } else {
                LOGGER.log(System.Logger.Level.ERROR, "Authorization failure. Request for"
                        + req.prologue().uriPath().absolute().path()
                        + " has failed, it was marked for explicit authorization, "
                        + "yet authorization was never called on security context. The "
                        + "method was invoked and may have changed data. Marking as internal server error."
                        + " Endpoint: " + ctx.serviceInfo().serviceType().fqName() + ", method: "
                        + ctx.elementInfo().elementName());
                res.status(Status.INTERNAL_SERVER_ERROR_500)
                        .send();
            }
        }
        var responseTracing = tracing.responseTracing();
        try {
            if (definition.isAudited()) {
                audit(req, res, resourceType, definition, securityContext);
            }
        } finally {
            responseTracing.finish();
        }
    }

    private void processSecurity(ServerRequest req,
                                 ServerResponse res,
                                 HttpSecurityInterceptorContext ictx,
                                 SecurityTracing tracing,
                                 HttpSecurityDefinition definition,
                                 SecurityContext securityContext) {
        authenticate(req, res, ictx, tracing, definition, securityContext);
        if (ictx.shouldFinish()) {
            return;
        }
        ictx.clearTrace();

        authorize(req, res, ictx, tracing, definition, securityContext);
    }

    private void authorize(ServerRequest req,
                           ServerResponse res,
                           HttpSecurityInterceptorContext ictx,
                           SecurityTracing tracing,
                           HttpSecurityDefinition definition,
                           SecurityContext securityContext) {

    }

    private void authenticate(ServerRequest req,
                              ServerResponse res,
                              HttpSecurityInterceptorContext ictx,
                              SecurityTracing tracing,
                              HttpSecurityDefinition definition,
                              SecurityContext securityContext) {

    }

    private void audit(ServerRequest req,
                       ServerResponse res,
                       String resourceName,
                       HttpSecurityDefinition methodSecurity,
                       SecurityContext securityContext) {
        AuditEvent.AuditSeverity auditSeverity;
        Status.Family family = res.status()
                .family();
        if (family == Status.Family.SUCCESSFUL) {
            auditSeverity = methodSecurity.auditOkSeverity();
        } else {
            auditSeverity = methodSecurity.auditErrorSeverity();
        }

        SecurityAuditEvent auditEvent = SecurityAuditEvent
                .audit(auditSeverity, methodSecurity.auditEventType(), methodSecurity.auditMessageFormat())
                .addParam(AuditEvent.AuditParam.plain("method", req.prologue().method().text()))
                .addParam(AuditEvent.AuditParam.plain("path", req.path().absolute().path()))
                .addParam(AuditEvent.AuditParam.plain("status", String.valueOf(res.status().codeText())))
                .addParam(AuditEvent.AuditParam.plain("subject",
                                                      securityContext.user()
                                                              .or(securityContext::service)
                                                              .orElse(SecurityContext.ANONYMOUS)))
                .addParam(AuditEvent.AuditParam.plain("transport", "http"))
                .addParam(AuditEvent.AuditParam.plain("resourceType", resourceName))
                .addParam(AuditEvent.AuditParam.plain("targetUri", req.requestedUri().toUri()));

        securityContext.audit(auditEvent);
    }

    private HttpSecurityDefinition methodSecurity(InvocationContext ctx) {
        Signature signature = Signature.create(ctx.serviceInfo().serviceType(), ctx.elementInfo());

        methodSecurityDefinitionLock.readLock().lock();
        try {
            var result = methodSecurityDefinitions.get(signature);
            if (result != null) {
                return result;
            }
        } finally {
            methodSecurityDefinitionLock.readLock().unlock();
        }
        methodSecurityDefinitionLock.writeLock().lock();
        try {
            var result = methodSecurityDefinitions.get(signature);
            if (result != null) {
                return result;
            }
            result = createMethodSecurity(ctx);
            methodSecurityDefinitions.put(signature, result);
            return result;
        } finally {
            methodSecurityDefinitionLock.writeLock().unlock();
        }
    }

    private HttpSecurityDefinition createMethodSecurity(InvocationContext ctx) {
        TypedElementInfo method = ctx.elementInfo();
        HttpSecurityDefinition typeSecurity = typeSecurity(ctx);
        HttpSecurityDefinition methodSecurity = typeSecurity.copy();

        securityAnnotations(methodSecurity, method.annotations());
        SecurityLevel currentLevel = methodSecurity.lastSecurityLevel();
        currentLevel = SecurityLevel.create(currentLevel)
                .withMethodName(method.elementName())
                .withMethodAnnotations(method.annotations())
                .build();
        methodSecurity.lastSecurityLevel(currentLevel);

        for (AnnotationAnalyzer analyzer : annotationAnalyzers) {
            methodSecurity.analyzerResponse(analyzer,
                                            analyzer.analyze(ctx.serviceInfo().serviceType(),
                                                             method.annotations(),
                                                             typeSecurity.analyzerResponse(analyzer)));
        }

        return methodSecurity;
    }

    private void securityAnnotations(HttpSecurityDefinition definition, List<Annotation> annotations) {
        Annotations.findFirst(Authenticated.TYPE, annotations)
                .ifPresent(definition::authenticated);
        Annotations.findFirst(Authorized.TYPE, annotations)
                .ifPresent(definition::authorized);
        Annotations.findFirst(Audited.TYPE, annotations)
                .ifPresent(definition::audited);
    }

    private HttpSecurityDefinition typeSecurity(InvocationContext ctx) {
        TypeName typeName = ctx.serviceInfo().serviceType();

        typeSecurityDefinitionLock.readLock().lock();
        try {
            var result = typeSecurityDefinitions.get(typeName);
            if (result != null) {
                return result;
            }
        } finally {
            typeSecurityDefinitionLock.readLock().unlock();
        }
        typeSecurityDefinitionLock.writeLock().lock();
        try {
            var result = typeSecurityDefinitions.get(typeName);
            if (result != null) {
                return result;
            }
            result = createTypeSecurity(ctx);
            typeSecurityDefinitions.put(typeName, result);
            return result;
        } finally {
            typeSecurityDefinitionLock.writeLock().unlock();
        }
    }

    private HttpSecurityDefinition createTypeSecurity(InvocationContext ctx) {
        HttpSecurityDefinition definition = new HttpSecurityDefinition();

        config.get("authenticate-annotated-only").asBoolean().ifPresent(definition::requiresAuthentication);
        config.get("authorize-annotated-only").asBoolean().ifPresent(definition::authorizeAnnotatedOnly);
        config.get("fail-on-failure-if-optional").asBoolean().ifPresent(definition::failOnFailureIfOptional);

        securityAnnotations(definition, ctx.typeAnnotations());

        SecurityLevel securityLevel = SecurityLevel.create(ctx.serviceInfo().serviceType().fqName())
                .withClassAnnotations(ctx.typeAnnotations())
                .build();
        definition.addSecurityLevel(securityLevel);

        for (AnnotationAnalyzer analyzer : annotationAnalyzers) {
            definition.analyzerResponse(analyzer, analyzer.analyze(ctx.serviceInfo().serviceType(), annotations));
        }

        return definition;
    }

    private Optional<Boolean> enabledFromAnnotation(InvocationContext ctx, TypeName annotationType) {
        // first go through explicit annotations on method, then method meta-annotations,
        // then type annotations, then type meta annotations

        for (Annotation annotation : ctx.elementInfo().annotations()) {
            if (annotation.typeName().equals(annotationType)) {
                return annotation.booleanValue();
            }
        }

        for (Annotation annotation : ctx.elementInfo().annotations()) {
            for (Annotation metaAnnotation : annotation.metaAnnotations()) {
                if (metaAnnotation.typeName().equals(annotationType)) {
                    return metaAnnotation.booleanValue();
                }
            }
        }

        for (Annotation annotation : ctx.typeAnnotations()) {
            if (annotation.typeName().equals(annotationType)) {
                return annotation.booleanValue();
            }
        }
        for (Annotation annotation : ctx.typeAnnotations()) {
            for (Annotation metaAnnotation : annotation.metaAnnotations()) {
                if (metaAnnotation.typeName().equals(annotationType)) {
                    return metaAnnotation.booleanValue();
                }
            }
        }

        return Optional.empty();
    }

    private record Signature(TypeName declaringType,
                             String methodName,
                             List<TypeName> parameterTypes) {
        static Signature create(TypeName declaringType, TypedElementInfo method) {
            return new Signature(declaringType,
                                 method.elementName(),
                                 method.parameterArguments().stream()
                                         .map(TypedElementInfo::typeName)
                                         .collect(Collectors.toUnmodifiableList()));
        }
    }
}
