package io.helidon.security;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import io.helidon.common.config.Config;
import io.helidon.security.spi.AuthenticationProvider;
import io.helidon.security.spi.AuthorizationProvider;
import io.helidon.security.spi.OutboundSecurityProvider;
import io.helidon.security.spi.ProviderSelectionPolicy;
import io.helidon.security.spi.SubjectMappingProvider;
import io.helidon.service.registry.Service;
import io.helidon.tracing.Tracer;

@Service.Provider
class SecurityService implements Security {
    private final Security delegate;

    SecurityService(Config config) {
        this.delegate = Security.builder()
                .config(config.get("security"))
                .build();
    }

    @Override
    public SecurityTime serverTime() {
        return delegate.serverTime();
    }

    @Override
    public SecurityContext.Builder contextBuilder(String id) {
        return delegate.contextBuilder(id);
    }

    @Override
    public SecurityContext createContext(String id) {
        return delegate.createContext(id);
    }

    @Override
    public Tracer tracer() {
        return delegate.tracer();
    }

    @Override
    public Collection<Class<? extends Annotation>> customAnnotations() {
        return delegate.customAnnotations();
    }

    @Override
    public Config configFor(String child) {
        return delegate.configFor(child);
    }

    @Override
    public String encrypt(String configurationName, byte[] bytesToEncrypt) {
        return delegate.encrypt(configurationName, bytesToEncrypt);
    }

    @Override
    public byte[] decrypt(String configurationName, String cipherText) {
        return delegate.decrypt(configurationName, cipherText);
    }

    @Override
    public String digest(String configurationName, byte[] bytesToDigest, boolean preHashed) {
        return delegate.digest(configurationName, bytesToDigest, preHashed);
    }

    @Override
    public String digest(String configurationName, byte[] bytesToDigest) {
        return delegate.digest(configurationName, bytesToDigest);
    }

    @Override
    public boolean verifyDigest(String configurationName, byte[] bytesToDigest, String digest, boolean preHashed) {
        return delegate.verifyDigest(configurationName, bytesToDigest, digest, preHashed);
    }

    @Override
    public boolean verifyDigest(String configurationName, byte[] bytesToDigest, String digest) {
        return delegate.verifyDigest(configurationName, bytesToDigest, digest);
    }

    @Override
    public Optional<String> secret(String configurationName) {
        return delegate.secret(configurationName);
    }

    @Override
    public String secret(String configurationName, String defaultValue) {
        return delegate.secret(configurationName, defaultValue);
    }

    @Override
    public SecurityEnvironment.Builder environmentBuilder() {
        return delegate.environmentBuilder();
    }

    @Override
    public Optional<SubjectMappingProvider> subjectMapper() {
        return delegate.subjectMapper();
    }

    @Override
    public boolean enabled() {
        return delegate.enabled();
    }

    @Override
    public void audit(String tracingId, AuditEvent event) {
        delegate.audit(tracingId, event);
    }

    @Override
    public ProviderSelectionPolicy providerSelectionPolicy() {
        return delegate.providerSelectionPolicy();
    }

    @Override
    public Optional<? extends AuthenticationProvider> resolveAtnProvider(String providerName) {
        return delegate.resolveAtnProvider(providerName);
    }

    @Override
    public Optional<AuthorizationProvider> resolveAtzProvider(String providerName) {
        return delegate.resolveAtzProvider(providerName);
    }

    @Override
    public List<? extends OutboundSecurityProvider> resolveOutboundProvider(String providerName) {
        return delegate.resolveOutboundProvider(providerName);
    }
}
