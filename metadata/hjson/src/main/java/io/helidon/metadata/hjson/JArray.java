package io.helidon.metadata.hjson;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * An immutable representation of JSON array.
 * The JSON array is handled as a {@link java.util.List} in this component.
 * All components of an array must be of the same type (restriction of this library, not of JSON).
 *
 * @param <T> type of the array components
 */
public final class JArray<T> implements JValue<List<T>> {
    private final JType type;
    private final List<JValue<T>> values;

    JArray(JType type, List<? extends JValue<T>> array) {
        this.type = type;
        this.values = List.copyOf(array);
    }

    /**
     * Create a new array of JSON Objects.
     *
     * @param objects object list
     * @return a new object array
     */
    public static JArray<JObject> createObjects(List<JObject> objects) {
        return new JArray<>(JType.OBJECT, objects);
    }

    /**
     * Create a new array of Strings.
     *
     * @param strings String list
     * @return a new string array
     */
    public static JArray<String> createStrings(List<String> strings) {
        List<JValues.StringValue> values = strings.stream()
                .map(JValues.StringValue::create)
                .collect(Collectors.toList());

        return new JArray<>(JType.STRING, values);
    }

    /**
     * Create a new array of Numbers.
     *
     * @param numbers {@link java.math.BigDecimal} list
     * @return a new number array
     */
    public static JArray<BigDecimal> createNumbers(List<BigDecimal> numbers) {
        return new JArray<>(JType.NUMBER, numbers.stream()
                .map(JValues.NumberValue::create)
                .collect(Collectors.toUnmodifiableList()));
    }

    /**
     * Create a new array of booleans.
     *
     * @param booleans boolean list
     * @return a new boolean array
     */
    public static JArray<Boolean> createBooleans(List<Boolean> booleans) {
        return new JArray<>(JType.BOOLEAN, booleans.stream()
                .map(JValues.BooleanValue::create)
                .collect(Collectors.toUnmodifiableList()));
    }

    /**
     * Create a new array of Numbers from long values.
     *
     * @param values long numbers
     * @return a new number array
     */
    public static JArray<BigDecimal> create(long... values) {
        List<BigDecimal> collect = LongStream.of(values)
                .mapToObj(BigDecimal::new)
                .collect(Collectors.toUnmodifiableList());
        return JArray.createNumbers(collect);
    }

    /**
     * Create a new array of Numbers from int values.
     *
     * @param values int numbers
     * @return a new number array
     */
    public static JArray<BigDecimal> create(int... values) {
        List<BigDecimal> collect = IntStream.of(values)
                .mapToObj(BigDecimal::new)
                .collect(Collectors.toUnmodifiableList());
        return JArray.createNumbers(collect);
    }

    /**
     * Create a new array of Numbers from double values.
     *
     * @param values double numbers
     * @return a new number array
     */
    public static JArray<BigDecimal> create(double... values) {
        List<BigDecimal> collect = DoubleStream.of(values)
                .mapToObj(BigDecimal::new)
                .collect(Collectors.toUnmodifiableList());
        return JArray.createNumbers(collect);
    }

    /**
     * Create a new array of Numbers from float values.
     *
     * @param values float numbers
     * @return a new number array
     */
    public static JArray<BigDecimal> create(float... values) {
        List<BigDecimal> list = new ArrayList<>(values.length);
        for (float value : values) {
            list.add(new BigDecimal(value));
        }

        return JArray.createNumbers(list);
    }

    /**
     * Create a new array from existing values.
     *
     * @param type type of the objects stored in the new array
     * @param values existing values
     * @return a new typed array
     * @param <T> type of the array to create
     */
    public static <T> JArray<T> create(JType type, List<JValue<T>> values) {
        return new JArray<>(type, values);
    }


    static JValue<?> create() {
        return new JArray<>(JType.OBJECT, List.of());
    }

    @Override
    public List<T> value() {
        return values.stream()
                .map(JValue::value)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public void write(PrintWriter metaWriter) {
        metaWriter.write('[');

        for (int i = 0; i < values.size(); i++) {
            values.get(i).write(metaWriter);
            if (i < (values.size() - 1)) {
                metaWriter.write(',');
            }
        }

        metaWriter.write(']');
    }

    @Override
    public boolean isArray() {
        return true;
    }

    @Override
    public JType type() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof JArray<?> jArray)) {
            return false;
        }
        return type == jArray.type && Objects.equals(values, jArray.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, values);
    }

    @Override
    public String toString() {
        return "[" +
                "type=" + type +
                ", values=" + values +
                ']';
    }
}
