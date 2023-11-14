package io.helidon.builder.test;

import io.helidon.builder.test.testsubjects.RuntimeTypeExample;
import io.helidon.builder.test.testsubjects.RuntimeTypeExampleConfig;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class RuntimeTypeExampleTest {
    @Test
    void sanityCheck() {
        RuntimeTypeExample runtimeType = RuntimeTypeExampleConfig.builder()
                .type("type")
                .build();

        assertThat(runtimeType.prototype().type(), is("type"));
    }
}
