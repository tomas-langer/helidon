package io.helidon.di.processor;

import java.util.Set;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.core.order.Ordered;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;

public class SecurityVisitor implements TypeElementVisitor<Object, Object> {
    private static final String ATN = "io.helidon.security.annotations.Authenticated";
    private static final String ATZ = "io.helidon.security.annotations.Authorized";
    private static final String AUDITED = "io.helidon.security.annotations.Audited";
    private static final String ROLES_ALLOWED = "javax.annotation.security.RolesAllowed";
    private static final String SECURED = "io.helidon.di.security.Secured";
    @NonNull
    @Override
    public VisitorKind getVisitorKind() {
        return VisitorKind.ISOLATING;
    }

    @Override
    public Set<String> getSupportedAnnotationNames() {
        return Set.of(ATN,
                      ATZ,
                      AUDITED,
                      ROLES_ALLOWED);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    @Override
    public void visitMethod(MethodElement element, VisitorContext context) {
        boolean authenticationRequired;
        boolean authenticationOptional;
        String authenticationProvider;
        boolean authorizationRequired;
        boolean authorizationExplicit;
        String authorizationProvider;
        boolean audited;
        String auditEventType;
        String auditMessageFormat;

        if (element.hasAnnotation(ATN)) {
            authenticationRequired = element.booleanValue(ATN).orElse(true);
            authenticationOptional = element.booleanValue(ATN, "optional").orElse(false);
            authenticationProvider = element.stringValue(ATN, "provider").orElse("");
        } else {
            authenticationRequired = false;
            authenticationOptional = false;
            authenticationProvider = "";
        }
        if (element.hasAnnotation(ATZ)) {
            authorizationRequired = element.booleanValue(ATZ).orElse(true) || element.hasAnnotation(ROLES_ALLOWED);
            authorizationExplicit = element.booleanValue(ATZ, "explicit").orElse(false);
            authorizationProvider = element.stringValue(ATZ, "provider").orElse("");
        } else {
            authorizationRequired = element.hasAnnotation(ROLES_ALLOWED);
            authorizationExplicit = false;
            authorizationProvider = "";
        }
        if (element.hasAnnotation(AUDITED)) {
            audited = true;
            auditEventType = element.stringValue(AUDITED).orElse("request");
            auditMessageFormat = element.stringValue(AUDITED, "messageFormat").orElse("%3$s %1$s \"%2$s\" %5$s %6$s requested by %4$s");
        } else {
            audited = false;
            auditEventType = "request";
            auditMessageFormat = "%3$s %1$s \"%2$s\" %5$s %6$s requested by %4$s";
        }

        element.annotate(SECURED, builder -> {
            builder.member("authenticationRequired", authenticationRequired);
            builder.member("authenticationOptional", authenticationOptional);
            builder.member("authenticationProvider", authenticationProvider);
            builder.member("authorizationRequired", authorizationRequired);
            // TODO finish
        });
    }

    /*
    boolean authorizationRequired() default false;
    boolean authorizeAll() default false;
    boolean authorizationExplicit() default false;
    String authorizationProvider() default "";
    boolean audited() default false;
    String auditEventType() default "";
    String auditMessageFormat() default "";

    AuditEvent.AuditSeverity auditOkSeverity() default AuditEvent.AuditSeverity.SUCCESS;
    AuditEvent.AuditSeverity auditErrorSeverity() default AuditEvent.AuditSeverity.ERROR;
     */
}
