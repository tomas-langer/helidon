/*
 * Copyright (c) 2024 Oracle and/or its affiliates.
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

package io.helidon.declarative.codegen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import io.helidon.codegen.CodegenException;

/**
 * Support for gathering constants names for a specific type (such as header names, HTTP statuses etc.) to
 * avoid duplication of constant values.
 *
 * @param <K> type of the constant, this type must be valid as a key in a {@link java.util.Map}
 */
public class Constants<K> {
    private final Map<K, String> constantNames = new HashMap<>();
    private final String namePrefix;

    private int index;

    private Constants(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    /**
     * Create new constants.
     * The prefix will be appended with an index, i.e. first constant name would be
     * {@code namePrefix0}.
     *
     * @param namePrefix name prefix of the constant names
     * @param <K>        type of the constant key (the unique values)
     * @return new constants instance
     */
    public static <K> Constants<K> create(String namePrefix) {
        return new Constants<>(namePrefix);
    }

    /**
     * Add all keys we need constants for.
     *
     * @param keys keys to add
     */
    public void addAll(List<K> keys) {
        keys.forEach(this::add);
    }

    /**
     * Add a single key we need constant for.
     *
     * @param key key to add
     * @return the assigned constant name
     */
    public String add(K key) {
        return constantNames.computeIfAbsent(key, it -> namePrefix + index++);
    }

    /**
     * A for each iterator triggered for each key and constant name.
     *
     * @param consumer consumer, first parameter is the key, second parameter is the constant name
     */
    public void forEach(BiConsumer<? super K, String> consumer) {
        constantNames.forEach(consumer);
    }

    /**
     * Get the constant name that exists, throws exception if not defined.
     *
     * @param key constant key
     * @return constant name
     * @throws io.helidon.codegen.CodegenException in case the constant is not defined
     */
    public String get(K key) {
        String value = constantNames.get(key);

        if (value == null) {
            throw new CodegenException("Expecting a constant for " + key + ", yet it was not defined. Prefix: " + namePrefix);
        }

        return value;
    }
}
