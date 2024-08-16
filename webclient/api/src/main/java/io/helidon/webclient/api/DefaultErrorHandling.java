package io.helidon.webclient.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.helidon.http.ClientRequestHeaders;
import io.helidon.http.HttpException;
import io.helidon.service.registry.Service;

@Service.Provider
class DefaultErrorHandling implements RestClient.ErrorHandling {
    private final List<RestClient.ErrorHandler> errorHandlers;

    DefaultErrorHandling(List<RestClient.ErrorHandler> errorHandlers) {
        List<RestClient.ErrorHandler> handlers = new ArrayList<>(errorHandlers);
        handlers.add(HttpErrorHandler.INSTANCE);
        this.errorHandlers = List.copyOf(handlers);
    }

    @Override
    public void handle(String uri, ClientRequestHeaders requestHeaders, HttpClientResponse response) {
        for (RestClient.ErrorHandler errorHandler : errorHandlers) {
            if (errorHandler.handles(uri, requestHeaders, response.status(), response.headers())) {
                var maybeException = errorHandler.handleError(uri, requestHeaders, response);
                if (maybeException.isPresent()) {
                    throw maybeException.get();
                }
            }
        }
    }

    @Override
    public void handle(String uri, ClientRequestHeaders requestHeaders, ClientResponseTyped<?> response, Class<?> type) {
        for (RestClient.ErrorHandler errorHandler : errorHandlers) {
            if (errorHandler.handles(uri, requestHeaders, response.status(), response.headers())) {
                var maybeException = errorHandler.handleError(uri, requestHeaders, response, type);
                if (maybeException.isPresent()) {
                    throw maybeException.get();
                }
            }
        }
    }

    private static class HttpErrorHandler implements RestClient.ErrorHandler {
        private static final HttpErrorHandler INSTANCE = new HttpErrorHandler();

        private HttpErrorHandler() {
        }

        @Override
        public Optional<? extends RuntimeException> handleError(String requestUri,
                                                                ClientRequestHeaders requestHeaders,
                                                                HttpClientResponse response) {
            return Optional.of(new HttpException("Failed when invoking a client call to " + requestUri
                                                         + ", status: " + response.status(),
                                                 response.status()));
        }

        @Override
        public Optional<? extends RuntimeException> handleError(String requestUri,
                                                                ClientRequestHeaders requestHeaders,
                                                                ClientResponseTyped<?> response,
                                                                Class<?> type) {
            return Optional.of(new HttpException("Failed when invoking a client call to " + requestUri
                                                         + ", status: " + response.status(),
                                                 response.status()));
        }
    }
}
