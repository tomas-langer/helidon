package io.helidon.builder.test.testsubjects;

import io.helidon.builder.api.Prototype;

@Prototype.Blueprint
interface RuntimeTypeExampleInterfaceConfigBlueprint extends Prototype.Factory<RuntimeTypeExampleInterface> {
    String type();
}
