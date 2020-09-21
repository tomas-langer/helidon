/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.webserver.junit5;

import java.util.Optional;

import io.helidon.config.Config;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@HelidonReactiveTest
@AddConfig(key = "key1", value = "value1")
@AddConfig(key = "key2", value = "value2")
class AddConfigTest {
    private final Config classConfig;

    AddConfigTest(Config classConfig) {
        this.classConfig = classConfig;
    }

    @Test
    @AddConfig(key = "key3", value = "value3")
    @AddConfig(key = "key4", value = "value4")
    void testAddConfig(Config config) {
        assertThat(classConfig, notNullValue());
        assertThat(config, notNullValue());

        assertThat(classConfig.get("key1").asString().get(), is("value1"));
        assertThat(classConfig.get("key2").asString().get(), is("value2"));
        assertThat(classConfig.get("key3").asString().asOptional(), is(Optional.empty()));
        assertThat(classConfig.get("key4").asString().asOptional(), is(Optional.empty()));

        assertThat(config.get("key1").asString().get(), is("value1"));
        assertThat(config.get("key2").asString().get(), is("value2"));
        assertThat(config.get("key3").asString().get(), is("value3"));
        assertThat(config.get("key4").asString().get(), is("value4"));
    }
}
