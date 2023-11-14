package io.helidon.builder.test.testsubjects;

import io.helidon.builder.api.Prototype;

@Prototype.Blueprint
interface RuntimeTypeExampleConfigBlueprint extends Prototype.Factory<RuntimeTypeExample> {
    String type();
}
