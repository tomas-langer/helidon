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
 * Support for gathering field names for a specific type (such as header names, HTTP statuses etc.) to
 * avoid duplication of values.
 *
 * @param <K> type of the field, this type must be valid as a key in a {@link java.util.Map}
 */
public class FieldNames<K> {
    private final Map<K, String> fieldNames = new HashMap<>();
    private final String namePrefix;

    private int index;

    private FieldNames(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    /**
     * Create new field names.
     * The prefix will be appended with an index, i.e. first field name would be
     * {@code namePrefix0}.
     *
     * @param namePrefix name prefix of the field names
     * @param <K>        type of the field (the unique values)
     * @return new field names instance
     */
    public static <K> FieldNames<K> create(String namePrefix) {
        return new FieldNames<>(namePrefix);
    }

    /**
     * Add all keys we need fields for.
     *
     * @param keys keys to add
     */
    public void addAll(List<K> keys) {
        keys.forEach(this::add);
    }

    /**
     * Add a single key we need field for.
     *
     * @param key key to add
     * @return the assigned field name
     */
    public String add(K key) {
        return fieldNames.computeIfAbsent(key, it -> namePrefix + index++);
    }

    /**
     * A for each iterator triggered for each key and field name.
     *
     * @param consumer consumer, first parameter is the key, second parameter is the field name
     */
    public void forEach(BiConsumer<? super K, String> consumer) {
        fieldNames.forEach(consumer);
    }

    /**
     * Get the field name that exists, throws exception if not defined.
     *
     * @param key field key
     * @return field name
     * @throws io.helidon.codegen.CodegenException in case the field is not defined
     */
    public String get(K key) {
        String value = fieldNames.get(key);

        if (value == null) {
            throw new CodegenException("Expecting a field name for " + key + ", yet it was not defined. Prefix: " + namePrefix);
        }

        return value;
    }
}
