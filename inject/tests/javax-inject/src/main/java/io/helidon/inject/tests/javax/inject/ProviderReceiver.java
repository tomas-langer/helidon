package io.helidon.inject.tests.javax.inject;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
class ProviderReceiver {
    private final Provider<NonSingletonService> provider;
    private final List<Provider<NonSingletonService>> listOfProviders;
    private final Optional<Provider<NonSingletonService>> optionalProvider;

    @Inject
    ProviderReceiver(Provider<NonSingletonService> provider,
                     List<Provider<NonSingletonService>> listOfProviders,
                     Optional<Provider<NonSingletonService>> optionalProvider) {
        this.provider = provider;
        this.listOfProviders = listOfProviders;
        this.optionalProvider = optionalProvider;
    }

    NonSingletonService nonSingletonService() {
        return provider.get();
    }

    List<NonSingletonService> listOfServices() {
        return listOfProviders.stream()
                .map(Provider::get)
                .toList();
    }

    Optional<NonSingletonService> optionalService() {
        return optionalProvider.map(Provider::get);
    }
}
