package io.helidon.builder.test.testsubjects;

import java.util.function.Consumer;

import io.helidon.builder.api.RuntimeType;

@RuntimeType.PrototypedBy(RuntimeTypeExampleInterfaceConfig.class)
public interface RuntimeTypeExampleInterface extends RuntimeType.Api<RuntimeTypeExampleInterfaceConfig> {

    static RuntimeTypeExampleInterface create(RuntimeTypeExampleInterfaceConfig prototype) {
        return new AnImplementation(prototype);
    }

    static RuntimeTypeExampleInterfaceConfig.Builder builder() {
        return RuntimeTypeExampleInterfaceConfig.builder();
    }

    static RuntimeTypeExampleInterface create(Consumer<RuntimeTypeExampleInterfaceConfig.Builder> consumer) {
        return builder().update(consumer).build();
    }

    class AnImplementation implements RuntimeTypeExampleInterface {
        private final RuntimeTypeExampleInterfaceConfig prototype;

        private AnImplementation(RuntimeTypeExampleInterfaceConfig prototype) {
            this.prototype = prototype;
        }

        @Override
        public RuntimeTypeExampleInterfaceConfig prototype() {
            return prototype;
        }
    }

}
