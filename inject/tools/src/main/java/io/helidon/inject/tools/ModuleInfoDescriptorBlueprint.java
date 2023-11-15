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

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;
import io.helidon.common.types.TypeName;

/**
 * Provides the basic formation for {@code module-info.java} creation and manipulation.
 *
 * @see java.lang.module.ModuleDescriptor
 */
@Prototype.Blueprint
@Prototype.CustomMethods(ModuleInfoDescriptorSupport.class)
interface ModuleInfoDescriptorBlueprint {

    /**
     * The default module name (i.e., "unnamed").
     */
    String DEFAULT_MODULE_NAME = "unnamed";

    /**
     * The base module-info name.
     */
    String MODULE_INFO_NAME = "module-info";

    /**
     * The java module-info name.
     */
    String DEFAULT_MODULE_INFO_JAVA_NAME = MODULE_INFO_NAME + ".java";

    /**
     * The module name.
     *
     * @return the module name
     */
    @Option.Default(DEFAULT_MODULE_NAME)
    String name();

    /**
     * Whether this module is declared as open module.
     *
     * @return whether this module is open
     */
    @ConfiguredOption("false")
    boolean open();

    /**
     * The header (i.e., copyright) comment - will appear at the very start of the output.
     *
     * @return the header comment
     */
    Optional<String> headerComment();

    /**
     * The description comment - will appear directly above the module's {@link #name()}.
     *
     * @return the description comment
     */
    Optional<String> descriptionComment();

    /**
     * The ordering applied.
     *
     * @return the ordering
     */
    @Option.Default("NATURAL")
    ModuleInfoOrdering ordering();

    /**
     * The items contained by this module-info.
     *
     * @return the items
     */
    @Option.Singular
    List<ModuleInfoItem> items();

    /**
     * The items that were not handled (due to parsing outages, etc.).
     *
     * @return the list of unhandled lines
     */
    @Option.Singular
    List<String> unhandledLines();

    /**
     * Any throwable/error that were encountered during parsing.
     *
     * @return optionally any error encountered during parsing
     */
    Optional<Throwable> error();

    /**
     * Returns {@code true} if last parsing operation was successful (i.e., if there were no instances of
     * {@link #unhandledLines()} or {@link #error()}'s encountered).
     *
     * @return true if any parsing of the given module-info descriptor appears to be full and complete
     */
    default boolean handled() {
        return error().isEmpty() && unhandledLines().isEmpty();
    }

    /**
     * Returns true if the name currently set is the same as the {@link #DEFAULT_MODULE_NAME}.
     *
     * @return true if the current name is the default name
     */
    default boolean isUnnamed() {
        return DEFAULT_MODULE_NAME.equals(name());
    }

    /**
     * Provides the ability to create a new merged descriptor using this as the basis, and then combining another into it
     * in order to create a new descriptor.
     *
     * @param another the other descriptor to merge
     * @return the merged descriptor
     */
    @SuppressWarnings("unchecked")
    default ModuleInfoDescriptor mergeCreate(ModuleInfoDescriptor another) {
        if (another == this) {
            throw new IllegalArgumentException("can't merge with self");
        }

        ModuleInfoDescriptor.Builder newOne = ModuleInfoDescriptor.builder((ModuleInfoDescriptor) this);
        for (ModuleInfoItem itemThere : another.items()) {
            Optional<ModuleInfoItem> itemHere = first(itemThere);
            if (itemHere.isPresent()) {
                int index = newOne.items().indexOf(itemHere.get());
                newOne.items().remove(index);
                ModuleInfoItem mergedItem = itemHere.get().mergeCreate(itemThere);
                newOne.items().add(index, mergedItem);
            } else {
                newOne.addItem(itemThere);
            }
        }

        return newOne.build();
    }

    /**
     * Retrieves the first item matching the target requested.
     *
     * @param item the item to find
     * @return the item or empty if not found
     */
    default Optional<ModuleInfoItem> first(ModuleInfoItem item) {
        return items().stream()
                .filter(it -> (item.uses() && it.uses())
                        || (item.opens() && it.opens())
                        || (item.exports() && it.exports())
                        || (item.provides() && it.provides())
                        || (item.requires() && it.requires()))
                .filter(it -> it.target().equals(item.target()))
                .findFirst();
    }

    /**
     * Returns the first export found in the module that is unqualified with any extra {@code to} declaration.
     *
     * @return the first package that is exported from this module, or empty if there are no exports appropriate
     */
    default Optional<String> firstUnqualifiedPackageExport() {
        return items().stream()
                .filter(item -> item.exports() && item.withOrTo().isEmpty())
                .map(ModuleInfoItem::target)
                .findFirst();
    }
}
