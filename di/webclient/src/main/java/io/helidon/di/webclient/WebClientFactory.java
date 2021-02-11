package io.helidon.di.webclient;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import io.helidon.common.http.Http;
import io.helidon.di.annotation.http.Client;
import io.helidon.media.common.MediaContext;
import io.helidon.webclient.WebClient;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.micronaut.context.BeanLocator;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.BootstrapContextCompatible;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.InjectionPoint;
import io.micronaut.inject.qualifiers.Qualifiers;

@Factory
class WebClientFactory {
    private static final ClientKey DEFAULT_CLIENT_KEY = new ClientKey(Http.Version.V1_1);
    private final Map<ClientKey, WebClient> webClients = new ConcurrentHashMap<>();

    private final BeanLocator beanLocator;
    private final MediaContext mediaContext;

    protected WebClientFactory(BeanLocator beanLocator, MediaContext mediaContext) {
        this.beanLocator = beanLocator;
        this.mediaContext = mediaContext;
    }

    /**
     * Creates a client build for the given configuration.
     *
     * @param configuration The configuration
     * @return The web client builder
     */
    @EachBean(WebClientConfig.class)
    protected WebClient.Builder clientBuilder(WebClientConfig configuration) {
        return configuration.clientBuilder();
    }

    @Bean
    @BootstrapContextCompatible
    @Primary
    protected WebClient webClient(@Nullable InjectionPoint<?> injectionPoint) {
        if (injectionPoint == null) {
            return resolveClient(beanLocator, DEFAULT_CLIENT_KEY);
        } else {
            AnnotationMetadata annotationMetadata = injectionPoint.getAnnotationMetadata();
            ClientKey key = getKey(annotationMetadata);
            return resolveClient(beanLocator, key);
        }
    }

    private WebClient resolveClient(BeanLocator beanLocator, ClientKey key) {
        return webClients.computeIfAbsent(key, clientKey -> {
            WebClient.Builder clientBuilder;
            if (clientKey.clientId == null) {
                // use the default bean if exists, otherwise just create a new one
                clientBuilder = beanLocator.findBean(WebClient.Builder.class)
                        .orElseGet(WebClient::builder);
            } else {
                clientBuilder = beanLocator.findBean(WebClient.Builder.class, Qualifiers.byName(clientKey.clientId))
                        .or(() -> beanLocator.findBean(WebClient.Builder.class))
                        .orElseGet(WebClient::builder);
            }
            clientBuilder.mediaContext(mediaContext);
            if (clientKey.endpoint != null) {
                clientBuilder.baseUri(clientKey.endpoint);
            }

            if (clientKey.path != null) {
                URI currentUri = clientBuilder.uri();
                if (currentUri == null) {
                    // TODO need to add identification of client (or injection point)
                    throw new IllegalStateException("Client configured with a path is missing endpoint configuration");
                }
                URI newUri = null;
                try {
                    newUri = new URI(currentUri.getScheme(),
                                     currentUri.getUserInfo(),
                                     currentUri.getHost(),
                                     currentUri.getPort(),
                                     joinPath(currentUri.getPath(), clientKey.path),
                                     currentUri.getQuery(),
                                     currentUri.getFragment());
                } catch (URISyntaxException e) {
                    throw new IllegalStateException("Failed to construct new web client URI", e);
                }
                clientBuilder.baseUri(newUri);
            }

            return clientBuilder.build();
        });
    }

    private String joinPath(String current, String configured) {
        if (current == null) {
            return configured;
        }
        StringBuilder result = new StringBuilder(current.length() + configured.length() + 1);
        result.append(current);
        boolean hasIt = current.endsWith("/");
        if (configured.startsWith("/")) {
            if (hasIt) {
                result.append(configured.substring(1));
            } else {
                result.append(configured);
            }
        }
        return result.toString();
    }

    private ClientKey getKey(AnnotationMetadata annotationMetadata) {
        String endpoint = annotationMetadata.stringValue(Client.class).orElse(null);
        String name = annotationMetadata.stringValue(Client.class, "name").orElse(null);
        String path = annotationMetadata.stringValue(Client.class, "basePath").orElse(null);
        Http.Version version = annotationMetadata.enumValue(Client.class, "version", Http.Version.class)
                .orElse(Http.Version.V1_1);

        return new ClientKey(endpoint, name, path, version);
    }

    /**
     * Client key.
     */
    @Internal
    static final class ClientKey {
        final String clientId;
        final String endpoint;
        final String path;
        final Http.Version httpVersion;

        public ClientKey(Http.Version httpVersion) {
            this(null, null, null, httpVersion);
        }

        ClientKey(String endpoint, String clientId, String path, Http.Version httpVersion) {
            this.endpoint = endpoint;
            this.clientId = clientId;
            this.path = path;
            this.httpVersion = httpVersion;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ClientKey clientKey = (ClientKey) o;
            return Objects.equals(clientId, clientKey.clientId) && Objects
                    .equals(endpoint, clientKey.endpoint) && Objects
                    .equals(path, clientKey.path) && httpVersion == clientKey.httpVersion;
        }

        @Override
        public int hashCode() {
            return Objects.hash(clientId, endpoint, path, httpVersion);
        }
    }
}
