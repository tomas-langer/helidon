/*
 * Copyright (c) 2022, 2023 Oracle and/or its affiliates.
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

package io.helidon.inject.tools;

import java.util.List;
import java.util.Optional;

import io.helidon.common.testing.junit5.OptionalMatcher;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModuleInfoDescriptorTest {
    @Test
    void innerCommentsNotSupported() {
        String moduleInfo = "module test {\nprovides /* inner comment */ cn;\n}";
        ToolsException te = assertThrows(ToolsException.class,
                                         () -> ModuleInfoDescriptor
                                                 .create(moduleInfo, ModuleInfoOrdering.NATURAL_PRESERVE_COMMENTS, true));
        assertThat(te.getMessage(),
                   equalTo("Unable to load or parse module-info: module test {\nprovides /* inner comment */ cn;\n}"));

        ModuleInfoDescriptor descriptor = ModuleInfoDescriptor.create(moduleInfo);
        assertThat(descriptor.handled(),
                   is(false));
        assertThat(descriptor.unhandledLines(),
                   contains("module test {"));
        assertThat(descriptor.error().orElseThrow().getMessage(),
                   equalTo("Unable to load or parse module-info: module test {\nprovides /* inner comment */ cn;\n}"));
    }

    @Test
    void loadCreateAndSave() {
        ModuleInfoDescriptor descriptor = ModuleInfoDescriptor
                .create(CommonUtils.loadStringFromResource("testsubjects/m0._java_"),
                        ModuleInfoOrdering.NATURAL, true);
        assertThat(descriptor.name(), is("io.helidon.inject"));

        String contents = CommonUtils.loadStringFromFile("target/test-classes/testsubjects/m0._java_").trim();
        descriptor = ModuleInfoDescriptor.create(contents, ModuleInfoOrdering.NATURAL_PRESERVE_COMMENTS, true);
        assertThat(descriptor.name(), is("io.helidon.inject"));
    }

    @Test
    void addIfAbsent() {
        ModuleInfoDescriptor.Builder builder = ModuleInfoDescriptor.builder();
        ModuleInfoUtil.addIfAbsent(builder, "external",
                                   () -> ModuleInfoItem.builder()
                                           .uses(true)
                                           .target("external")
                                           .addPrecomment("    // 1")
                                           .build());
        ModuleInfoUtil.addIfAbsent(builder, "external",
                                   () -> ModuleInfoItem.builder()
                                           .uses(true)
                                           .target("external")
                                           .addPrecomment("    // 2")
                                           .build());
        ModuleInfoDescriptor descriptor = builder.build();
        assertThat(descriptor.name(), is("unnamed"));
        List<ModuleInfoItem> items = descriptor.items();
        assertThat(items, hasSize(1));
        ModuleInfoItem item = items.getFirst();
        assertThat(item.uses(), is(true));
        assertThat(item.target(), is("external"));
    }

    @Test
    void testConfigModule() {
        ModuleInfoDescriptor module = ModuleInfoDescriptor.create(ModuleInfoDescriptorTest.class.getResourceAsStream(
                "/testsubjects/config-module.txt"));

        assertThat(module.name(), is("io.helidon.config"));
        assertThat(module.open(), is(true));
        // all requires
        assertThat(module.items()
                           .stream()
                           .filter(ModuleInfoItem::requires)
                           .map(ModuleInfoItem::target)
                           .toList()
                , contains("io.helidon.inject.api",
                           "io.helidon.inject.runtime",
                           "io.helidon.common.features.api",
                           "jakarta.inject",
                           "jakarta.annotation",
                           "io.helidon.common.config",
                           "io.helidon.common.media.type",
                           "io.helidon.common"));
        // all exports
        assertThat(module.items()
                           .stream()
                           .filter(ModuleInfoItem::exports)
                           .map(ModuleInfoItem::target)
                           .toList()
                , contains("io.helidon.config",
                           "io.helidon.config.spi"));
        // all uses
        assertThat(module.items()
                           .stream()
                           .filter(ModuleInfoItem::uses)
                           .map(ModuleInfoItem::target)
                           .toList()
                , contains("io.helidon.config.spi.ConfigMapperProvider",
                           "io.helidon.config.spi.ConfigParser"));

        // provides (just checking one, to make it easier)
        Optional<ModuleInfoItem> provides = module.items()
                .stream()
                .filter(ModuleInfoItem::provides)
                .findFirst();
        assertThat(provides, OptionalMatcher.optionalPresent());
        ModuleInfoItem providesItem = provides.get();
        assertThat(providesItem.target(), is("io.helidon.config.spi.ConfigParser"));
        assertThat(providesItem.withOrTo(), contains("io.helidon.config.PropertiesConfigParser",
                                                     "io.helidon.config.RandomConfigParser"));
        // opens (just one again)
        Optional<ModuleInfoItem> opens = module.items()
                .stream()
                .filter(ModuleInfoItem::opens)
                .findFirst();
        assertThat(opens, OptionalMatcher.optionalPresent());
        ModuleInfoItem opensItem = opens.get();
        assertThat(opensItem.target(), is("io.helidon.config"));
        assertThat(opensItem.withOrTo(), contains("weld.core.impl",
                                                  "io.helidon.microprofile.cdi"));

    }
}
