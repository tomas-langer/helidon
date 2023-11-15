package io.helidon.inject.api;

import io.helidon.builder.api.Prototype;

final class BlueprintsSupport {
    private BlueprintsSupport() {
    }

    static class ActivationRequestDecorator implements Prototype.BuilderDecorator<ActivationRequest.BuilderBase<?, ?>> {
        ActivationRequestDecorator() {
        }

        @Override
        public void decorate(ActivationRequest.BuilderBase<?, ?> target) {
            if (target.targetPhase().isEmpty()) {
                target.targetPhase(InjectionServices.terminalActivationPhase());
            }
        }
    }
}
