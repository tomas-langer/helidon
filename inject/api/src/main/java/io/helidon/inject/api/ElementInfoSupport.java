package io.helidon.inject.api;

import java.util.Optional;

import io.helidon.builder.api.Prototype;
import io.helidon.common.types.ElementKind;

final class ElementInfoSupport {
    private ElementInfoSupport() {
    }

    static class BuilderDecorator implements Prototype.BuilderDecorator<ElementInfo.BuilderBase<?, ?>> {
        BuilderDecorator() {
        }

        @Override
        public void decorate(ElementInfo.BuilderBase<?, ?> target) {
            Optional<ElementKind> elementKind = target.elementKind();
            if (elementKind.isPresent()) {
                if (elementKind.get() == ElementKind.CONSTRUCTOR) {
                    target.elementName(ElementInfoBlueprint.CONSTRUCTOR);
                }
            }
        }
    }
}
