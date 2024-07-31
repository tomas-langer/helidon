package io.helidon.webserver.security;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.helidon.common.config.Config;
import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.common.types.TypeName;
import io.helidon.http.Status;
import io.helidon.security.AuthenticationResponse;
import io.helidon.security.EndpointConfig;
import io.helidon.security.Security;
import io.helidon.security.SecurityClientBuilder;
import io.helidon.security.SecurityContext;
import io.helidon.security.SecurityResponse;
import io.helidon.security.integration.common.AtnTracing;
import io.helidon.security.integration.common.AtzTracing;
import io.helidon.security.integration.common.SecurityTracing;
import io.helidon.service.inject.api.InjectRegistry;
import io.helidon.service.inject.api.Lookup;
import io.helidon.service.inject.api.Qualifier;
import io.helidon.service.registry.Service;
import io.helidon.webserver.http.EntryPointFilter;
import io.helidon.webserver.http.FilterChain;

@Service.Provider
class SecurityEntryPoint implements EntryPointFilter {
    private static final System.Logger LOGGER = System.getLogger(SecurityEntryPoint.class.getName());

    private final Config config;
    private final InjectRegistry registry;
    private final Supplier<Security> security;

    SecurityEntryPoint(Config config, InjectRegistry registry, Supplier<Security> security) {
        this.config = config;
        this.registry = registry;
        this.security = security;
    }

    @Override
    public void filter(FilterChain filterChain, EntryPointContext ctx) {
        Context context = Contexts.context().orElseGet(ctx.request()::context);

        SecurityContext securityContext = context.get(SecurityContext.class)
                .orElseThrow(() -> new IllegalStateException("SecurityContext is not present."));

        context.register(securityContext);

        TypeName declaringType = ctx.declaringType();
        String methodName = ctx.methodName();
        List<TypeName> typeNames = ctx.parameterTypes();
        String name = declaringType + "." + methodName + typeNames.stream()
                .map(TypeName::fqName)
                .collect(Collectors.joining(","));

        if (!config.get(name).get("security.enabled").asBoolean().orElse(false)) {
            if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
                LOGGER.log(System.Logger.Level.TRACE, "Security gate not enabled: " + name);
            }
            filterChain.proceed();
            return;
        }

        // now find the generated handler
        SecurityGate gate = registry.get(Lookup.builder()
                                                 .addContract(SecurityGate.class)
                                                 .addQualifier(Qualifier.createNamed(name))
                                                 .build());

        boolean done = true;
        if (gate.isAuthenticated()) {
            done = securityContext.isAuthenticated();
        }
        if (gate.isAuthorized() && done) {
            done = securityContext.isAuthorized();
        }

        if (done) {
            if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
                LOGGER.log(System.Logger.Level.TRACE,
                           "Security gate not used, as security already handled by configured path");
            }
            filterChain.proceed();
            return;
        }

        SecurityTracing tracing = SecurityTracing.get();
        tracing.securityContext(securityContext);

        securityContext.endpointConfig(EndpointConfig.builder()
                                               .securityLevels(List.of(gate.securityLevel()))
                                               .build());

        if (gate.isAuthenticated() && !securityContext.isAuthenticated()) {
            // authenticate
            if (!authenticate(ctx, context, securityContext, gate, tracing.atnTracing())) {
                return;
            }
        }

        if (gate.isAuthorized() && !securityContext.isAuthorized()) {
            // authorize
            if (!authorize(ctx, context, securityContext, gate, tracing.atzTracing())) {
                return;
            }
        }

        filterChain.proceed();
    }

    private boolean authorize(EntryPointContext ctx,
                              Context context,
                              SecurityContext securityContext, SecurityGate gate, AtzTracing atzTracing) {
        return false;
    }

    private boolean authenticate(EntryPointContext ctx,
                                 Context context,
                                 SecurityContext securityContext,
                                 SecurityGate gate,
                                 AtnTracing atnTracing) {
        //authenticate request
        boolean success = false;
        Throwable thrown = null;
        String traceDescription = null;

        try {
            SecurityClientBuilder<AuthenticationResponse> clientBuilder = securityContext
                    .atnClientBuilder()
                    .optional(gate.authenticationOptional())
                    .tracingSpan(atnTracing.findParent().orElse(null));

            gate.authenticator().ifPresent(clientBuilder::explicitProvider);

            AuthenticationResponse response = clientBuilder.submit();
            SecurityResponse.SecurityStatus responseStatus = response.status();
            atnTracing.logStatus(responseStatus);

            switch(responseStatus) {
            case SUCCESS -> {
                //everything is fine, we can continue with processing
                context.register(SecurityHttpFeature.CONTEXT_RESPONSE_HEADERS, response.responseHeaders());
            }
            case SUCCESS_FINISH -> {
                int status = response.statusCode().orElse(Status.OK_200.code());
                abortRequest(context, response, status, Map.of());
                success = false;
            }
            case FAILURE -> {
            }
            case FAILURE_FINISH -> {
            }
            case ABSTAIN -> {
            }
            }
        } finally {
            if (success) {
                securityContext.user().ifPresent(atnTracing::logUser);
                securityContext.service().ifPresent(atnTracing::logService);
                atnTracing.finish();
            } else {
                if (thrown == null) {
                    atnTracing.error(traceDescription);
                } else {
                    atnTracing.error(thrown);
                }
            }
        }
    }
}
