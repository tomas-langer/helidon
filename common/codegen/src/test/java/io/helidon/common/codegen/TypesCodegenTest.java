package io.helidon.common.codegen;

import java.lang.annotation.ElementType;
import java.util.List;

import io.helidon.common.types.Annotation;
import io.helidon.common.types.TypeName;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class TypesCodegenTest {
    @Test
    void testIt() {
        Annotation annotation = Annotation.builder()
                .typeName(TypeName.create("io.helidon.RandomAnnotation"))
                .putValue("string", "value1")
                .putValue("boolean", true)
                .putValue("long", 49L)
                .putValue("double", 49.0D)
                .putValue("integer", 49)
                .putValue("byte", (byte) 49)
                .putValue("char", 'x')
                .putValue("short", (short) 49)
                .putValue("float", 49.0F)
                .putValue("class", TypesCodegenTest.class)
                .putValue("type", TypeName.create(TypesCodegenTest.class))
                .putValue("enum", ElementType.FIELD)
                .putValue("lstring", List.of("value1", "value2"))
                .putValue("lboolean", List.of(true, false))
                .putValue("llong", List.of(49L, 50L))
                .putValue("ldouble", List.of(49.0, 50.0))
                .putValue("linteger", List.of(49, 50))
                .putValue("lbyte", List.of((byte) 49, (byte) 50))
                .putValue("lchar", List.of('x', 'y'))
                .putValue("lshort", List.of((short) 49, (short) 50))
                .putValue("lfloat", List.of(49.0F, 50.0F))
                .putValue("lclass", List.of(TypesCodegenTest.class, TypesCodegenTest.class))
                .putValue("ltype",
                          List.of(TypeName.create(TypesCodegenTest.class), TypeName.create(TypesCodegenTest.class)))
                .putValue("lenum", List.of(ElementType.FIELD, ElementType.MODULE))
                .build();
        String createString = TypesCodeGen.toCreate(annotation);

        assertThat(createString,
                   is("@io.helidon.common.types.Annotation@.builder().typeName(@io.helidon.common.types.TypeName@.create(\"io"
                              + ".helidon.RandomAnnotation\")).putValue(\"string\", \"value1\").putValue(\"boolean\", true)"
                              + ".putValue(\"long\", 49L).putValue(\"double\", 49.0D).putValue(\"integer\", 49).putValue"
                              + "(\"byte\", (byte)49).putValue(\"char\", 'x').putValue(\"short\", (short)49).putValue"
                              + "(\"float\", 49.0F).putValue(\"class\", @io.helidon.common.types.TypeName@.create(\"io.helidon"
                              + ".common.codegen.TypesCodegenTest\")).putValue(\"type\", @io.helidon.common.types.TypeName@"
                              + ".create(\"io.helidon.common.codegen.TypesCodegenTest\")).putValue(\"enum\", @java.lang"
                              + ".annotation.ElementType@.FIELD).putValue(\"lstring\", @java.util.List@.of(\"value1\","
                              + "\"value2\")).putValue(\"lboolean\", @java.util.List@.of(true,false)).putValue(\"llong\", @java"
                              + ".util.List@.of(49L,50L)).putValue(\"ldouble\", @java.util.List@.of(49.0D,50.0D)).putValue"
                              + "(\"linteger\", @java.util.List@.of(49,50)).putValue(\"lbyte\", @java.util.List@.of((byte)49,"
                              + "(byte)50)).putValue(\"lchar\", @java.util.List@.of('x','y')).putValue(\"lshort\", @java.util"
                              + ".List@.of((short)49,(short)50)).putValue(\"lfloat\", @java.util.List@.of(49.0F,50.0F))"
                              + ".putValue(\"lclass\", @java.util.List@.of(@io.helidon.common.types.TypeName@.create(\"io"
                              + ".helidon.common.codegen.TypesCodegenTest\"),@io.helidon.common.types.TypeName@.create(\"io"
                              + ".helidon.common.codegen.TypesCodegenTest\"))).putValue(\"ltype\", @java.util.List@.of(@io"
                              + ".helidon.common.types.TypeName@.create(\"io.helidon.common.codegen.TypesCodegenTest\"),@io"
                              + ".helidon.common.types.TypeName@.create(\"io.helidon.common.codegen.TypesCodegenTest\")))"
                              + ".putValue(\"lenum\", @java.util.List@.of(@java.lang.annotation.ElementType@.FIELD,@java.lang"
                              + ".annotation.ElementType@.MODULE)).build()"));
    }
}
