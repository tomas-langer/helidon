package io.helidon.common.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import io.helidon.common.buffers.BufferData;
import io.helidon.common.buffers.Bytes;
import io.helidon.common.buffers.DataReader;

class Parser {
    private static final int MAX_FIELD_LENGTH = 64000;
    private static final byte COMMA = (byte) ',';
    private static final byte QUOTES = (byte) '"';
    private static final byte ARRAY_START = (byte) '[';
    private static final byte ARRAY_END = (byte) ']';
    private static final byte OBJECT_START = (byte) '{';
    private static final byte OBJECT_END = (byte) '}';
    private static final byte BACKSLASH = (byte) '\\';

    private final DataReader reader;
    private int position;

    private Parser(DataReader reader) {
        this.reader = reader;
    }

    static JValue<?> parse(InputStream stream) {
        DataReader dr = new DataReader(() -> {
            byte[] buffer = new byte[1024];
            try {
                int num = stream.read(buffer);
                if (num > 0) {
                    return buffer;
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            return null;
        });
        return new Parser(dr).read(true);
    }

    private JValue<?> read(boolean topLevel) {
        byte next = skipWhitespace();
        if (next == ARRAY_START) {
            return readArray();
        } else if (next == OBJECT_START) {
            return readObject();
        }
        if (topLevel) {
            throw new JException("Index: " + position
                                         + ": failed to parse JSON, invalid object/array opening character: \n"
                                         + BufferData.create(new byte[] {next}).debugDataHex());
        }
        if (next == QUOTES) {
            return readString("Object");
        }
        return readValue();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private JValue<?> readArray() {
        skip(); // skip [

        List values = new ArrayList();
        JValue firstValue = null;

        while (true) {
            byte next = skipWhitespace();
            if (next == ARRAY_END) {
                // end of array
                return arrayEnd(firstValue, values);
            }

            if (firstValue == null) {
                firstValue = switch (next) {
                    case OBJECT_START -> readObject();
                    case QUOTES -> readString("Array");
                    default -> readValue();
                };
                values.add(firstValue);
            } else {
                values.add(switch (firstValue.type()) {
                    case OBJECT -> readObject();
                    case STRING -> readString("Field");
                    case BOOLEAN -> readBoolean();
                    case NUMBER -> readNumber();
                });
            }
            next = skipWhitespace();
            if (next == COMMA) {
                skip(); // ,
            } else {
                // this must be array end
                next = skipWhitespace();
                if (next == ARRAY_END) {
                    return arrayEnd(firstValue, values);
                } else {
                    throw new JException("Index: " + position
                                                 + ": value not followed by a comma, and array does not end");
                }
            }
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private JValue<?> arrayEnd(JValue firstValue, List values) {
        skip(); // skip ]
        if (firstValue == null) {
            return JArray.create();
        }
        return JArray.create(firstValue.type(), values);
    }

    private JValue<JObject> readObject() {
        skip(); // skip {

        JObject object = JObject.create();

        while (true) {
            byte next = skipWhitespace();
            if (next == OBJECT_END) {
                skip(); // skip }
                return object;
            }
            // now we have "key": value (may be an object, value, string)
            String key = readKey();
            skipWhitespace();
            next = read();
            if (next != Bytes.COLON_BYTE) {
                throw new JException("Index: " + position
                                             + ": key is not followed by a colon. Key: " + BufferData.create(key).debugDataHex());
            }
            skipWhitespace();
            // the value may be object, array, value
            JValue<?> value = read(false);
            object.set(key, value);
            next = skipWhitespace();
            if (next == COMMA) {
                skip(); // ,
            } else {
                // this must be object end
                next = skipWhitespace();
                if (next == OBJECT_END) {
                    skip(); // skip }
                    return object;
                } else {
                    throw new JException("Index: " + position
                                                 + ": value not followed by a comma, and object does not end");
                }
            }
        }
    }

    private String readKey() {
        byte read = reader.lookup();
        if (read != QUOTES) {
            throw new JException("Index: " + position
                                         + ": keys must be quoted, invalid beginning of key");
        }
        return readString("Key").value();
    }

    private JValue<String> readString(String type) {
        skip(); // skip "

        // now go until the first unescaped quotes
        ByteArrayOutputStream value = new ByteArrayOutputStream();
        int count = 0;
        boolean escaping = false;
        while (count < MAX_FIELD_LENGTH) {
            byte next = reader.read();

            if (!escaping && next == QUOTES) {
                return JValues.StringValue.create(value.toString(StandardCharsets.UTF_8));
            }
            if (escaping) {
                escaping = false;
                byte toWrite = switch ((char) (next & 0xff)) {
                    case 'f' -> (byte) '\f';
                    case 'n' -> (byte) '\n';
                    case 'r' -> (byte) '\r';
                    case 't' -> (byte) '\t';
                    case '\\' -> (byte) '\\';
                    case '\"' -> (byte) '\"';
                    default -> throw new JException("Index " + position
                                                            + ": invalid escape char after backslash: '"
                                                            + (char) (next & 0xff) + "'");
                };
                value.write(toWrite);
            } else if (next == BACKSLASH) {
                escaping = true;
            } else {
                value.write(next);
            }
            count++;
        }

        throw new JException("Index: " + position
                                     + ": " + type + " failed to find end quotes, or length is bigger than allowed. Max length: "
                                     + MAX_FIELD_LENGTH + " bytes");
    }

    private JValue<Boolean> readBoolean() {
        String value = toNonStringValueEnd();

        if ("true".equals(value) || "false".equals(value)) {
            return JValues.BooleanValue.create(Boolean.parseBoolean(value));
        }
        throw new JException("Index: " + position
                                     + ": next value is expected to be a boolean, but the array contained wrong type: " +
                                     BufferData.create(value).debugDataHex());
    }

    private JValue<BigDecimal> readNumber() {
        String value = toNonStringValueEnd();

        try {
            return JValues.NumberValue.create(new BigDecimal(value));
        } catch (NumberFormatException e) {
            throw new JException("Index: " + position
                                         + ": next value is expected to be a number, but the array contained wrong type: " +
                                         BufferData.create(value).debugDataHex());
        }
    }

    private JValue<?> readValue() {
        String value = toNonStringValueEnd();

        // true | false, integer, double
        if ("true".equals(value) || "false".equals(value)) {
            return JValues.BooleanValue.create(Boolean.parseBoolean(value));
        }
        if ("null".equals(value)) {
            throw new JException("Index: " + position
                                         + "null values are not supported by this parser");
        }
        try {
            return JValues.NumberValue.create(new BigDecimal(value));
        } catch (NumberFormatException e) {
            throw new JException("Index: " + position
                                         + ": cannot parse JSON value into a number. Data: "
                                         + BufferData.create(value).debugDataHex());
        }
    }

    private String toNonStringValueEnd() {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();

        // anything from here to next whitespace or comma (separating values)
        while (true) {
            byte next = reader.lookup();
            if (whitespace(next)) {
                break;
            }
            if (next == COMMA || next == ARRAY_END || next == OBJECT_END) {
                break;
            }
            skip();
            bo.write(next);
        }

        return bo.toString(StandardCharsets.US_ASCII);
    }

    private byte skipWhitespace() {
        while (true) {
            byte lookup = reader.lookup();
            if (whitespace(lookup)) {
                skip();
            } else {
                return lookup;
            }
        }
    }

    private boolean whitespace(byte lookup) {
        return switch (lookup) {
            case Bytes.SPACE_BYTE, Bytes.TAB_BYTE, Bytes.CR_BYTE, Bytes.LF_BYTE -> true;
            default -> false;
        };
    }

    private void skip() {
        reader.skip(1);
        position++;
    }

    private byte read() {
        byte r = reader.read();
        position++;
        return r;
    }
}
