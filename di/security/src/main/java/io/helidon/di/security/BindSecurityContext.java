package io.helidon.di.security;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Singleton;

import io.helidon.common.context.Context;
import io.helidon.common.context.Contexts;
import io.helidon.di.webserver.HttpExchange;
import io.helidon.di.webserver.HttpParameterBinder;
import io.helidon.security.Security;
import io.helidon.security.SecurityContext;
import io.helidon.webserver.WebServer;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Order;
import io.micronaut.core.convert.ArgumentConversionContext;
import io.micronaut.core.type.Argument;

@Singleton
@Requires(classes = WebServer.class)
@Internal
@Order(110)
public class BindSecurityContext implements HttpParameterBinder<SecurityContext> {
    private final AtomicLong counter = new AtomicLong();
    private final Security security;

    public BindSecurityContext(Security security) {
        this.security = security;
    }

    @Override
    public boolean supports(Argument<?> argument) {
        return argument.getType() == SecurityContext.class;
    }

    @Override
    public BindingResult<SecurityContext> bind(ArgumentConversionContext<SecurityContext> context, HttpExchange source) {
        Optional<Context> maybeHelidonContext = Contexts.context();
        if (maybeHelidonContext.isEmpty()) {
            throw new IllegalStateException("Cannot inject SecurityContext when not running in io.helidon.common.context"
                                                    + ".Context");
        }
        Context helidonContext = maybeHelidonContext.get();
        Optional<SecurityContext> securityContext = helidonContext.get(SecurityContext.class);

        if (securityContext.isPresent()) {
            return () -> securityContext;
        }

        SecurityContext result = security.createContext("di-security-context-" + counter.incrementAndGet());
        helidonContext.register(result);
        Optional<SecurityContext> optionalResult = Optional.of(result);
        return () -> optionalResult;
    }
}
