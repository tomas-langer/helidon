package io.helidon.declarative.codegen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import io.helidon.codegen.CodegenException;

public class Constants<K> {
    private final Map<K, String> constantNames = new HashMap<>();
    private final String namePrefix;

    private int index;

    private Constants(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public static <K> Constants<K> create(String namePrefix) {
        return new Constants<>(namePrefix);
    }

    public void addAll(List<K> keys) {
        keys.forEach(this::add);
    }

    public void add(K key) {
        constantNames.computeIfAbsent(key, it -> namePrefix + index++);
    }

    public void forEach(BiConsumer<? super K, String> consumer) {
        constantNames.forEach(consumer);
    }

    /**
     * Get the constant name that exists, throws exception if not defined.
     *
     * @param key constant key
     * @return constant name
     * @throws io.helidon.codegen.CodegenException in case the constant is not defined
     * @see #find(Object)
     */
    public String get(K key) {
        String value = constantNames.get(key);

        if (value == null) {
            throw new CodegenException("Expecting a constant for " + key + ", yet it was not defined. Prefix: " + namePrefix);
        }

        return value;
    }


    public Optional<String> find(K key) {
        return Optional.ofNullable(constantNames.get(key));
    }
}
