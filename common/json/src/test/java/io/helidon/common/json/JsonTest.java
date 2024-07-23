package io.helidon.common.json;

import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import static io.helidon.common.testing.junit5.OptionalMatcher.optionalEmpty;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsonTest {
    @Test
    void testWrongTypes() {
        JObject object = JObject.create();
        object.set("number", 1);
        object.set("string", "hi");
        object.setLongs("numbers", List.of(1L, 2L, 3L));
        object.setStrings("strings", List.of("hi", "there"));

        assertThrows(JException.class,
                     () -> object.getString("number"));
        assertThrows(JException.class,
                     () -> object.getBoolean("number"));
        assertThrows(JException.class,
                     () -> object.getDouble("string"));
        assertThrows(JException.class,
                     () -> object.getObject("string"));
        assertThrows(JException.class,
                     () -> object.getStrings("string"));
        assertThrows(JException.class,
                     () -> object.getObjects("string"));
        assertThrows(JException.class,
                     () -> object.getBooleans("string"));
        assertThrows(JException.class,
                     () -> object.getNumbers("string"));

        assertThrows(JException.class,
                     () -> object.getString("strings"));
        assertThrows(JException.class,
                     () -> object.getBoolean("strings"));
        assertThrows(JException.class,
                     () -> object.getDouble("strings"));
        assertThrows(JException.class,
                     () -> object.getObject("strings"));
        assertThrows(JException.class,
                     () -> object.getStrings("numbers"));
        assertThrows(JException.class,
                     () -> object.getObjects("strings"));
        assertThrows(JException.class,
                     () -> object.getBooleans("strings"));
        assertThrows(JException.class,
                     () -> object.getNumbers("strings"));
    }
    @Test
    void testReads() {
        JObject empty = JObject.create();

        assertThat(empty.getBoolean("anyKey"), optionalEmpty());
        assertThat(empty.getBoolean("anyKey", true), is(true));

        assertThat(empty.getInt("anyKey"), optionalEmpty());
        assertThat(empty.getInt("anyKey", 876), is(876));

        assertThat(empty.getDouble("anyKey"), optionalEmpty());
        assertThat(empty.getDouble("anyKey", 876), is(876d));

        assertThat(empty.getStrings("anyKey"), optionalEmpty());
        assertThat(empty.getObjects("anyKey"), optionalEmpty());
        assertThat(empty.getNumbers("anyKey"), optionalEmpty());

        var returned = empty.unset("anyKey");
        assertThat(returned, sameInstance(empty));
    }

    @Test
    void testSetObjects() {
        JObject empty = JObject.create();
        JObject one = JObject.create();
        JObject two = JObject.create();

        JObject returned = empty.setObjects("objects", List.of(one, two));
        assertThat(returned, sameInstance(empty));

        List<JObject> objects = empty.getObjects("objects")
                .orElseThrow(() -> new IllegalStateException("objects key should be filled with array"));

        assertThat(objects, hasItems(one, two));
    }

    @Test
    void testSetNumbers() {
        JObject empty = JObject.create();
        BigDecimal one = new BigDecimal(14);
        BigDecimal two = new BigDecimal(15);

        JObject returned = empty.setNumbers("numbers", List.of(one, two));
        assertThat(returned, sameInstance(empty));

        List<BigDecimal> objects = empty.getNumbers("numbers")
                .orElseThrow(() -> new IllegalStateException("numbers key should be filled with array"));

        assertThat(objects, hasItems(one, two));
    }

    @Test
    void testSetBooleans() {
        JObject empty = JObject.create();

        JObject returned = empty.setBooleans("booleans", List.of(true, false, true));
        assertThat(returned, sameInstance(empty));

        List<Boolean> objects = empty.getBooleans("booleans")
                .orElseThrow(() -> new IllegalStateException("booleans key should be filled with array"));

        assertThat(objects, hasItems(true, false, true));
    }

    @Test
    void testWriteStringArray() {
        JArray<String> array = JArray.createStrings(List.of("a", "b", "c"));

        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            array.write(pw);
        }

        String string = sw.toString();
        assertThat(string, is("[\"a\",\"b\",\"c\"]"));
    }

    @Test
    void testWriteLongArray() {
        JArray<BigDecimal> array = JArray.create(2L, 3L, 4L);

        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            array.write(pw);
        }

        String string = sw.toString();
        assertThat(string, is("[2,3,4]"));
    }

    @Test
    void testWriteDoubleArray() {
        JArray<BigDecimal> array = JArray.createNumbers(List.of(new BigDecimal(2), new BigDecimal(3), new BigDecimal("4.2")));

        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            array.write(pw);
        }

        String string = sw.toString();
        assertThat(string, is("[2,3,4.2]"));
    }

    @Test
    void testWriteBooleanArray() {
        JArray<Boolean> array = JArray.createBooleans(List.of(true, false, true));

        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            array.write(pw);
        }

        String string = sw.toString();
        assertThat(string, is("[true,false,true]"));
    }

    @Test
    void testWriteObjectArray() {
        JObject first = JObject.create()
                .set("string", "value")
                .set("long", 4L)
                .set("double", 4d)
                .set("boolean", true)
                .setStrings("strings", List.of("a", "b"))
                .setLongs("longs", List.of(1L, 2L))
                .setDoubles("doubles", List.of(1.5d, 2.5d))
                .setBooleans("booleans", List.of(true, false));
        JObject second = JObject.create()
                .set("string", "value2");

        JArray<JObject> array = JArray.createObjects(List.of(first, second));

        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            array.write(pw);
        }

        String string = sw.toString();
        String expected = "[{\"string\":\"value\","
                + "\"long\":4,"
                + "\"double\":4,"
                + "\"boolean\":true,"
                + "\"strings\":[\"a\",\"b\"],"
                + "\"longs\":[1,2],"
                + "\"doubles\":[1.5,2.5],"
                + "\"booleans\":[true,false]},"
                + "{\"string\":\"value2\"}]";
        assertThat(string, is(expected));

        JValue<?> read = JValue.read(new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)));
        assertThat(read.type(), is(JType.OBJECT));
        assertThat(read.isArray(), is(true));
        JArray<JObject> objects = read.asObjectArray();
        List<JObject> value = objects.value();
        assertThat(value, hasSize(2));
        JObject readFirst = value.get(0);
        assertThat(readFirst, is(first));
        JObject readSecond = value.get(1);
        assertThat(readSecond, is(second));

    }
}
