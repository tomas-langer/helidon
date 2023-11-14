package io.helidon.builder.test.testsubjects;

import java.util.function.Consumer;

import io.helidon.builder.api.RuntimeType;

@RuntimeType.PrototypedBy(RuntimeTypeExampleConfig.class)
public class RuntimeTypeExample implements RuntimeType.Api<RuntimeTypeExampleConfig> {
    private final RuntimeTypeExampleConfig prototype;

    private RuntimeTypeExample(RuntimeTypeExampleConfig prototype) {
        this.prototype = prototype;
    }

    static RuntimeTypeExample create(RuntimeTypeExampleConfig prototype) {
        return new RuntimeTypeExample(prototype);
    }

    static RuntimeTypeExampleConfig.Builder builder() {
        return RuntimeTypeExampleConfig.builder();
    }

    static RuntimeTypeExample create(Consumer<RuntimeTypeExampleConfig.Builder> consumer) {
        return builder().update(consumer).build();
    }

    @Override
    public RuntimeTypeExampleConfig prototype() {
        return prototype;
    }
}
