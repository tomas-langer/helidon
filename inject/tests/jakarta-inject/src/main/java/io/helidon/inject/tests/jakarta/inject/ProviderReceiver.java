package io.helidon.inject.tests.jakarta.inject;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

@Singleton
class ProviderReceiver {
    private final Provider<NonSingletonService> provider;
    private final List<Provider<NonSingletonService>> listOfProviders;
    private final Optional<Provider<NonSingletonService>> optionalProvider;
    private final AContract contract;

    @Inject
    ProviderReceiver(Provider<NonSingletonService> provider,
                     List<Provider<NonSingletonService>> listOfProviders,
                     Optional<Provider<NonSingletonService>> optionalProvider,
                     AContract contract) {
        this.provider = provider;
        this.listOfProviders = listOfProviders;
        this.optionalProvider = optionalProvider;
        this.contract = contract;
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

    AContract contract() {
        return contract;
    }
}
