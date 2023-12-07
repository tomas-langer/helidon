package io.helidon.inject.tests.helidon.inject;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import io.helidon.inject.service.Inject;

@Inject.Singleton
class ProviderReceiver {
    private final Supplier<NonSingletonService> provider;
    private final List<Supplier<NonSingletonService>> listOfProviders;
    private final Optional<Supplier<NonSingletonService>> optionalProvider;
    private final AContract contract;

    @Inject.Point
    ProviderReceiver(Supplier<NonSingletonService> provider,
                     List<Supplier<NonSingletonService>> listOfProviders,
                     Optional<Supplier<NonSingletonService>> optionalProvider,
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
                .map(Supplier::get)
                .toList();
    }

    Optional<NonSingletonService> optionalService() {
        return optionalProvider.map(Supplier::get);
    }

    AContract contract() {
        return contract;
    }
}
