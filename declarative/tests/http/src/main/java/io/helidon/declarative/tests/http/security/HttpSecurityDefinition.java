package io.helidon.declarative.tests.http.security;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import io.helidon.common.types.Annotation;
import io.helidon.security.AuditEvent;
import io.helidon.security.SecurityLevel;
import io.helidon.security.annotations.Audited;
import io.helidon.security.providers.common.spi.AnnotationAnalyzer;

class HttpSecurityDefinition {
    private final Map<AnnotationAnalyzer, AnnotationAnalyzer.AnalyzerResponse> analyzerResponses = new IdentityHashMap<>();
    private final List<SecurityLevel> securityLevels = new ArrayList<>();

    private boolean requiresAuthentication;
    private boolean failOnFailureIfOptional;
    private boolean authnOptional;
    private boolean authorizeByDefault = true;
    private boolean atzExplicit;
    private String authenticator;
    private String authorizer;
    private boolean audited;
    private String auditEventType;
    private String auditMessageFormat;
    private AuditEvent.AuditSeverity auditOkSeverity;
    private AuditEvent.AuditSeverity auditErrorSeverity;
    private Boolean requiresAuthorization;

    String auditMessageFormat() {
        return auditMessageFormat;
    }

    String auditEventType() {
        return auditEventType;
    }

    AuditEvent.AuditSeverity auditErrorSeverity() {
        return auditErrorSeverity;
    }

    AuditEvent.AuditSeverity auditOkSeverity() {
        return auditOkSeverity;
    }

    boolean isAudited() {
        return audited;
    }

    boolean noSecurity() {
        boolean authorize = requiresAuthorization != null && requiresAuthorization;
        boolean authenticate = requiresAuthentication;
        boolean audit = audited;
        return !authenticate && !authorize && !audit && !authorizeByDefault;
    }

    AnnotationAnalyzer.AnalyzerResponse analyzerResponse(AnnotationAnalyzer analyzer) {
        return analyzerResponses.get(analyzer);
    }

    void addSecurityLevel(SecurityLevel securityLevel) {
        securityLevels.add(securityLevel);
    }

    SecurityLevel lastSecurityLevel() {
        return securityLevels.getLast();
    }

    void lastSecurityLevel(SecurityLevel securityLevel) {
        securityLevels.set(securityLevels.size() - 1, securityLevel);
    }

    HttpSecurityDefinition copy() {
        HttpSecurityDefinition result = new HttpSecurityDefinition();
        result.requiresAuthentication = this.requiresAuthentication;
        result.requiresAuthorization = this.requiresAuthorization;
        result.failOnFailureIfOptional = this.failOnFailureIfOptional;
        result.authnOptional = this.authnOptional;
        result.authenticator = this.authenticator;
        result.authorizer = this.authorizer;
        result.securityLevels.addAll(this.securityLevels);
        result.authorizeByDefault = this.authorizeByDefault;
        result.atzExplicit = this.atzExplicit;

        return result;
    }

    void analyzerResponse(AnnotationAnalyzer analyzer, AnnotationAnalyzer.AnalyzerResponse response) {
        analyzerResponses.put(analyzer, response);

        switch (response.authenticationResponse()) {
        case REQUIRED -> {
            requiresAuthentication = true;
            authnOptional = false;
        }
        case OPTIONAL -> {
            requiresAuthentication = true;
            authnOptional = true;
        }
        case FORBIDDEN -> {
            requiresAuthentication = false;
            authnOptional = false;
        }
        default -> {
        }
        }

        if (this.requiresAuthorization == null) {
            this.requiresAuthorization = switch (response.authorizationResponse()) {
                case REQUIRED, OPTIONAL -> true;
                case FORBIDDEN -> false;
                default -> null;
            };
        }

        this.authenticator = response.authenticator().orElse(this.authenticator);
        this.authorizer = response.authorizer().orElse(this.authorizer);
    }

    List<SecurityLevel> securityLevels() {
        return securityLevels;
    }

    void authenticated(Annotation annotation) {
        this.requiresAuthentication = annotation.booleanValue().orElse(true);
        this.authenticator = annotation.stringValue("provider")
                .filter(Predicate.not(String::isBlank))
                .orElse(null);
        this.authnOptional = annotation.booleanValue("optional").orElse(false);
    }

    void authorized(Annotation annotation) {
        this.requiresAuthorization = annotation.booleanValue().orElse(true);
        this.authorizer = annotation.stringValue("provider")
                .filter(Predicate.not(String::isBlank))
                .orElse(null);
        this.atzExplicit = annotation.booleanValue("explicit").orElse(false);
    }

    void audited(Annotation annotation) {
        this.audited = true;
        this.auditEventType = checkDefault(auditEventType,
                                           annotation.stringValue().orElse(Audited.DEFAULT_EVENT_TYPE),
                                           Audited.DEFAULT_EVENT_TYPE);
        this.auditMessageFormat = checkDefault(auditMessageFormat,
                                               annotation.stringValue("messageFormat").orElse(Audited.DEFAULT_MESSAGE_FORMAT),
                                               Audited.DEFAULT_MESSAGE_FORMAT);
        this.auditOkSeverity = checkDefault(auditOkSeverity,
                                            annotation.enumValue("okSeverity", AuditEvent.AuditSeverity.class)
                                                    .orElse(Audited.DEFAULT_OK_SEVERITY),
                                            Audited.DEFAULT_OK_SEVERITY);
        this.auditErrorSeverity = checkDefault(auditErrorSeverity,
                                               annotation.enumValue("errorSeverity", AuditEvent.AuditSeverity.class)
                                                       .orElse(Audited.DEFAULT_ERROR_SEVERITY),
                                               Audited.DEFAULT_ERROR_SEVERITY);
    }

    void failOnFailureIfOptional(boolean failOnFailureIfOptional) {
        this.failOnFailureIfOptional = failOnFailureIfOptional;
    }

    void authorizeAnnotatedOnly(boolean annotatedOnly) {
        this.authorizeByDefault = !annotatedOnly;
    }

    void requiresAuthentication(boolean requires) {
        this.requiresAuthentication = requires;
    }

    boolean atzExplicit() {
        return atzExplicit;
    }

    private <T> T checkDefault(T currentValue, T annotValue, T defaultValue) {
        if (null == currentValue) {
            return annotValue;
        }

        if (currentValue.equals(defaultValue)) {
            return annotValue;
        }

        return currentValue;
    }
}
