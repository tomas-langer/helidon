package io.helidon.common.json;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * A JSON value (may of types of {@link io.helidon.common.json.JType}).
 */
public sealed interface JValue<T> permits JValues.StringValue,
                                          JValues.NumberValue,
                                          JValues.BooleanValue,
                                          JObject,
                                          JArray {
    /**
     * Read a JSON value from an input stream.
     *
     * @param stream input stream to read JSON from
     * @return a parsed value, either non-array of type {@link io.helidon.common.json.JType#OBJECT},
     *         or an array
     * @see #asObject()
     * @see #asObjectArray()
     * @see #isArray()
     */
    static JValue<?> read(InputStream stream) {
        return Parser.parse(stream);
    }

    /**
     * Write the JSON value.
     *
     * @param writer writer to write to
     */
    void write(PrintWriter writer);

    /**
     * Value.
     *
     * @return the value
     */
    T value();

    /**
     * Whether this type represents an array.
     *
     * @return if this is an array
     */
    default boolean isArray() {
        return false;
    }

    /**
     * Type of this value, if this is {@link #isArray()}, then type of elements of that array.
     *
     * @return type of this value
     */
    JType type();

    /**
     * Get an object array from this parsed value.
     *
     * @return object array, or this object as an array
     * @throws io.helidon.common.json.JException in case this object is not of type {@link io.helidon.common.json.JType#OBJECT}
     */
    @SuppressWarnings("unchecked")
    default JArray<JObject> asObjectArray() {
        if (type() != JType.OBJECT) {
            throw new JException("Attempting to read object of type " + type() + " as an Object array");
        }
        if (this instanceof JArray<?> a) {
            return (JArray<JObject>) a;
        }
        if (this instanceof JObject j) {
            return JArray.createObjects(List.of(j));
        }
        throw new JException("Attempting to read class " + getClass().getName() + " as an Object array");
    }

    /**
     * Get an object from this parsed value.
     *
     * @return this value as an object
     * @throws io.helidon.common.json.JException in case this object is not of type {@link io.helidon.common.json.JType#OBJECT}
     */
    default JObject asObject() {
        if (type() != JType.OBJECT) {
            throw new JException("Attempting to read object of type " + type() + " as an Object");
        }
        if (isArray()) {
            throw new JException("Attempting to read array of type " + type() + " as an Object");
        }
        if (this instanceof JObject j) {
            return j;
        }
        throw new JException("Attempting to read class " + getClass().getName() + " as an Object");
    }
}
