/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
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

package io.helidon.metadata.hjson;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * JSON Object.
 * <p>
 * A mutable representation of a JSON object.
 */
public final class JObject implements JValue<JObject> {
    private final Map<String, JValue<?>> values = new LinkedHashMap<>();

    private JObject() {
    }

    /**
     * Create a new, empty instance.
     *
     * @return new empty instance
     */
    public static JObject create() {
        return new JObject();
    }

    @Override
    public JObject value() {
        return this;
    }

    /**
     * Get a boolean value.
     *
     * @param key key under this object
     * @return boolean value if present
     * @throws io.helidon.common.json.JException in case the key exists, but is not a {@code boolean}
     */
    public Optional<Boolean> getBoolean(String key) {
        return Optional.ofNullable(getObjectBoolean(key, null));
    }

    /**
     * Get a boolean value with default if not defined.
     *
     * @param key          key under this object
     * @param defaultValue default value to use if the key does not exist
     * @return boolean value, or default value if the key does not exist
     * @throws io.helidon.common.json.JException in case the key exists, but is not a
     *                                           {@link io.helidon.common.json.JType#BOOLEAN}
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return getObjectBoolean(key, defaultValue);
    }

    /**
     * Get object value.
     *
     * @param key key under this object
     * @return object value if present
     * @throws io.helidon.common.json.JException in case the key exists, but is not a
     *                                           {@link io.helidon.common.json.JType#OBJECT}
     */
    public Optional<JObject> getObject(String key) {
        JValue<?> jValue = values.get(key);
        if (jValue == null) {
            return Optional.empty();
        }
        if (jValue.isArray()) {
            throw new JException("Requesting key \"" + key + "\" as an object, but it is an array");
        }
        if (jValue.type() != JType.OBJECT) {
            throw new JException("Requesting key \"" + key + "\" as an object, but it is of type " + jValue.type());
        }
        return Optional.of((JObject) jValue);
    }

    /**
     * Get string value.
     *
     * @param key key under this object
     * @return string value if present
     * @throws io.helidon.common.json.JException in case the key exists, but is not a
     *                                           {@link io.helidon.common.json.JType#STRING}
     */
    public Optional<String> getString(String key) {
        String value = getString(key, null);
        return Optional.ofNullable(value);
    }

    /**
     * Get a string value with default if not defined.
     *
     * @param key          key under this object
     * @param defaultValue default value to use if the key does not exist
     * @return string value, or default value if the key does not exist
     * @throws io.helidon.common.json.JException in case the key exists, but is not a
     *                                           {@link io.helidon.common.json.JType#STRING}
     */
    public String getString(String key, String defaultValue) {
        JValue<?> jValue = values.get(key);
        if (jValue == null) {
            return defaultValue;
        }
        if (jValue.isArray()) {
            throw new JException("Requesting key \"" + key + "\" as a string, but it is an array");
        }
        if (jValue.type() != JType.STRING) {
            throw new JException("Requesting key \"" + key + "\" as a string, but it is of type " + jValue.type());
        }
        return (String) jValue.value();
    }

    /**
     * Get int value.
     *
     * @param key key under this object
     * @return int value if present, from {@link java.math.BigDecimal#intValue()}
     * @throws io.helidon.common.json.JException in case the key exists, but is not a
     *                                           {@link io.helidon.common.json.JType#NUMBER}
     */
    public Optional<Integer> getInt(String key) {
        return Optional.ofNullable(getObjectInt(key, null));
    }

    /**
     * Get an int value with default if not defined.
     *
     * @param key          key under this object
     * @param defaultValue default value to use if the key does not exist
     * @return int value, or default value if the key does not exist
     * @throws io.helidon.common.json.JException in case the key exists, but is not a
     *                                           {@link io.helidon.common.json.JType#NUMBER}
     * @see #getInt(String)
     */
    public int getInt(String key, int defaultValue) {
        return getObjectInt(key, defaultValue);
    }

    /**
     * Get double value.
     *
     * @param key key under this object
     * @return double value if present, from {@link java.math.BigDecimal#doubleValue()}
     * @throws io.helidon.common.json.JException in case the key exists, but is not a
     *                                           {@link io.helidon.common.json.JType#NUMBER}
     */
    public Optional<Double> getDouble(String key) {
        return Optional.ofNullable(getObjectDouble(key, null));
    }

    /**
     * Get a double value with default if not defined.
     *
     * @param key          key under this object
     * @param defaultValue default value to use if the key does not exist
     * @return double value, or default value if the key does not exist
     * @throws io.helidon.common.json.JException in case the key exists, but is not a
     *                                           {@link io.helidon.common.json.JType#NUMBER}
     * @see #getDouble(String)
     */
    public double getDouble(String key, double defaultValue) {
        return getObjectDouble(key, defaultValue);
    }

    /**
     * Get string array value.
     *
     * @param key key under this object
     * @return string array value, if the key exists
     * @throws io.helidon.common.json.JException in case the key exists, but is not a
     *                                           {@link io.helidon.common.json.JType#STRING}
     * @throws io.helidon.common.json.JException in case the key exists, but is not {@link #isArray()}
     */
    @SuppressWarnings("unchecked")
    public Optional<List<String>> getStrings(String key) {
        JValue<?> jValue = values.get(key);
        if (jValue == null) {
            return Optional.empty();
        }
        if (!jValue.isArray()) {
            throw new JException("Requesting key \"" + key + "\" as a json array, but it is: " + jValue.type());
        }
        JArray<String> value = (JArray<String>) jValue;
        if (value.value().isEmpty()) {
            // empty values are parsed always as objects, next if would fail
            return Optional.of(List.of());
        }
        if (jValue.type() != JType.STRING) {
            throw new JException("Requesting key \"" + key + "\" as array of strings, but it is array of type " + jValue.type());
        }
        return Optional.of(value.value());
    }

    /**
     * Get object array value.
     *
     * @param key key under this object
     * @return object array value, if the key exists
     * @throws io.helidon.common.json.JException in case the key exists, but is not a
     *                                           {@link io.helidon.common.json.JType#OBJECT}
     * @throws io.helidon.common.json.JException in case the key exists, but is not {@link #isArray()}
     */
    @SuppressWarnings("unchecked")
    public Optional<List<JObject>> getObjects(String key) {
        JValue<?> jValue = values.get(key);
        if (jValue == null) {
            return Optional.empty();
        }
        if (!jValue.isArray()) {
            throw new JException("Requesting key \"" + key + "\" as a json array, but it is: " + jValue.type());
        }
        if (jValue.type() != JType.OBJECT) {
            throw new JException("Requesting key \"" + key + "\" as array of objects, but it is array of type " + jValue.type());
        }
        return Optional.of(((JArray<JObject>) jValue).value());
    }

    /**
     * Get number array value.
     *
     * @param key key under this object
     * @return number array value, if the key exists
     * @throws io.helidon.common.json.JException in case the key exists, but is not a
     *                                           {@link io.helidon.common.json.JType#NUMBER}
     * @throws io.helidon.common.json.JException in case the key exists, but is not {@link #isArray()}
     */
    @SuppressWarnings("unchecked")
    public Optional<List<BigDecimal>> getNumbers(String key) {
        JValue<?> jValue = values.get(key);
        if (jValue == null) {
            return Optional.empty();
        }
        if (!jValue.isArray()) {
            throw new JException("Requesting key \"" + key + "\" as a json array, but it is: " + jValue.type());
        }
        var value = (JArray<BigDecimal>) jValue;
        if (value.value().isEmpty()) {
            // empty values are parsed always as objects, next if would fail
            return Optional.of(List.of());
        }
        if (jValue.type() != JType.NUMBER) {
            throw new JException("Requesting key \"" + key + "\" as array of numbers, but it is array of type " + jValue.type());
        }
        return Optional.of(value.value());
    }

    /**
     * Get boolean array value.
     *
     * @param key key under this object
     * @return boolean array value, if the key exists
     * @throws io.helidon.common.json.JException in case the key exists, but is not a
     *                                           {@link io.helidon.common.json.JType#BOOLEAN}
     * @throws io.helidon.common.json.JException in case the key exists, but is not {@link #isArray()}
     */
    @SuppressWarnings("unchecked")
    public Optional<List<Boolean>> getBooleans(String key) {
        JValue<?> jValue = values.get(key);
        if (jValue == null) {
            return Optional.empty();
        }
        if (!jValue.isArray()) {
            throw new JException("Requesting key \"" + key + "\" as a json array, but it is: " + jValue.type());
        }
        var value = (JArray<Boolean>) jValue;
        if (value.value().isEmpty()) {
            // empty values are parsed always as objects, next if would fail
            return Optional.of(List.of());
        }
        if (jValue.type() != JType.BOOLEAN) {
            throw new JException("Requesting key \"" + key + "\" as array of booleans, but it is array of type " + jValue.type());
        }
        return Optional.of(value.value());
    }

    /**
     * Unset an existing value assigned to the key.
     * This method does not care if the key is mapped or not.
     *
     * @param key key to unset
     * @return updated instance (this instance)
     */
    public JObject unset(String key) {
        Objects.requireNonNull(key, "key cannot be null");

        values.remove(key);
        return this;
    }

    /**
     * Set a string value.
     *
     * @param key   key to set
     * @param value value to assign to the key
     * @return updated instance (this instance)
     */
    public JObject set(String key, String value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");

        values.put(key, JValues.StringValue.create(value));
        return this;
    }

    /**
     * Set a boolean value.
     *
     * @param key   key to set
     * @param value value to assign to the key
     * @return updated instance (this instance)
     */
    public JObject set(String key, boolean value) {
        Objects.requireNonNull(key, "key cannot be null");

        values.put(key, JValues.BooleanValue.create(value));
        return this;
    }

    /**
     * Set a double value.
     *
     * @param key   key to set
     * @param value value to assign to the key
     * @return updated instance (this instance)
     */
    public JObject set(String key, double value) {
        Objects.requireNonNull(key, "key cannot be null");

        values.put(key, JValues.NumberValue.create(new BigDecimal(value)));
        return this;
    }

    /**
     * Set an int value.
     *
     * @param key   key to set
     * @param value value to assign to the key
     * @return updated instance (this instance)
     */
    public JObject set(String key, int value) {
        Objects.requireNonNull(key, "key cannot be null");

        values.put(key, JValues.NumberValue.create(new BigDecimal(value)));
        return this;
    }

    /**
     * Set a long value.
     *
     * @param key   key to set
     * @param value value to assign to the key
     * @return updated instance (this instance)
     */
    public JObject set(String key, long value) {
        Objects.requireNonNull(key, "key cannot be null");

        values.put(key, JValues.NumberValue.create(new BigDecimal(value)));
        return this;
    }

    /**
     * Set a {@link java.math.BigDecimal} value.
     *
     * @param key   key to set
     * @param value value to assign to the key
     * @return updated instance (this instance)
     */
    public JObject set(String key, BigDecimal value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");

        values.put(key, JValues.NumberValue.create(value));
        return this;
    }

    /**
     * Set an array of objects.
     *
     * @param key   key to set
     * @param value value to assign to the key
     * @return updated instance (this instance)
     */
    public JObject setObjects(String key, List<JObject> value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");

        values.put(key, JArray.createObjects(value));
        return this;
    }

    /**
     * Set an array of strings.
     *
     * @param key   key to set
     * @param value value to assign to the key
     * @return updated instance (this instance)
     */
    public JObject setStrings(String key, List<String> value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");

        values.put(key, JArray.createStrings(value));
        return this;
    }

    /**
     * Set an array of longs.
     *
     * @param key   key to set
     * @param value value to assign to the key
     * @return updated instance (this instance)
     */
    public JObject setLongs(String key, List<Long> value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");

        values.put(key, JArray.createNumbers(value.stream()
                                                     .map(BigDecimal::new)
                                                     .collect(Collectors.toUnmodifiableList())));
        return this;
    }

    /**
     * Set an array of doubles.
     *
     * @param key   key to set
     * @param value value to assign to the key
     * @return updated instance (this instance)
     */
    public JObject setDoubles(String key, List<Double> value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");
        values.put(key, JArray.createNumbers(value.stream()
                                                     .map(BigDecimal::new)
                                                     .collect(Collectors.toUnmodifiableList())));
        return this;
    }

    /**
     * Set an array of numbers (as {@link java.math.BigDecimal)}.
     *
     * @param key   key to set
     * @param value value to assign to the key
     * @return updated instance (this instance)
     */
    public JObject setNumbers(String key, List<BigDecimal> value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");

        values.put(key, JArray.createNumbers(value));
        return this;
    }

    /**
     * Set an array of booleans.
     *
     * @param key   key to set
     * @param value value to assign to the key
     * @return updated instance (this instance)
     */
    public JObject setBooleans(String key, List<Boolean> value) {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(value, "value cannot be null");

        values.put(key, JArray.createBooleans(value));
        return this;
    }

    @Override
    public void write(PrintWriter writer) {
        Objects.requireNonNull(writer);

        writer.write('{');
        AtomicBoolean first = new AtomicBoolean(true);

        values.forEach((key, value) -> {
            writeNext(writer, first);
            writer.write('\"');
            writer.write(key);
            writer.write("\":");
            value.write(writer);
        });

        writer.write('}');
    }

    @Override
    public JType type() {
        return JType.OBJECT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JObject jObject)) {
            return false;
        }
        return Objects.equals(values, jObject.values);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(values);
    }

    @Override
    public String toString() {
        return "{"
                + values +
                '}';
    }

    void set(String key, JValue<?> value) {
        values.put(key, value);
    }

    private void writeNext(PrintWriter metaWriter, AtomicBoolean first) {
        if (first.get()) {
            first.set(false);
            return;
        }
        metaWriter.write(',');
    }

    private Boolean getObjectBoolean(String key, Boolean defaultValue) {
        JValue<?> jValue = values.get(key);
        if (jValue == null) {
            return defaultValue;
        }
        if (jValue.isArray()) {
            throw new JException("Requesting key \"" + key + "\" as a boolean, but it is an array");
        }
        if (jValue.type() != JType.BOOLEAN) {
            throw new JException("Requesting key \"" + key + "\" as a boolean, but it is of type " + jValue.type());
        }
        return (Boolean) jValue.value();
    }

    private Integer getObjectInt(String key, Integer defaultValue) {
        JValue<?> jValue = values.get(key);
        if (jValue == null) {
            return defaultValue;
        }
        if (jValue.isArray()) {
            throw new JException("Requesting key \"" + key + "\" as an int, but it is an array");
        }
        if (jValue.type() != JType.NUMBER) {
            throw new JException("Requesting key \"" + key + "\" as an int, but it is of type " + jValue.type());
        }
        return ((BigDecimal) jValue.value()).intValue();
    }

    private Double getObjectDouble(String key, Double defaultValue) {
        JValue<?> jValue = values.get(key);
        if (jValue == null) {
            return defaultValue;
        }
        if (jValue.isArray()) {
            throw new JException("Requesting key \"" + key + "\" as a double, but it is an array");
        }
        if (jValue.type() != JType.NUMBER) {
            throw new JException("Requesting key \"" + key + "\" as a double, but it is of type " + jValue.type());
        }
        return ((BigDecimal) jValue.value()).doubleValue();
    }
}


