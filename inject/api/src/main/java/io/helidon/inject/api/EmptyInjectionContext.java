package io.helidon.inject.api;

import java.util.NoSuchElementException;

class EmptyInjectionContext implements InjectionContext {
    static final InjectionContext EMPTY = new EmptyInjectionContext();

    private EmptyInjectionContext() {
    }

    @Override
    public <T> T param(IpId<T> paramId) {
        throw new NoSuchElementException("Cannot resolve injection parameter, service descriptor did not declare any "
                                                 + "dependencies");
    }
}
