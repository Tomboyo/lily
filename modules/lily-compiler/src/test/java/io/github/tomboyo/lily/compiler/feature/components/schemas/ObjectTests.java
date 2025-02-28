package io.github.tomboyo.lily.compiler.feature.components.schemas;

import static io.github.tomboyo.lily.compiler.feature.components.schemas.TestSupport.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.tomboyo.lily.compiler.LilyExtension;
import io.github.tomboyo.lily.compiler.LilyExtension.LilyTestSupport;
import io.github.tomboyo.lily.compiler.cg.Mustache;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/*
 * These tests cover all components.schemas elements with type object. These schemas are compiled to models.
 */
public class ObjectTests {

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @CsvSource(
      value = {
        // style, format, java expression, json literal
        "boolean; null; java.lang.Boolean.TRUE; true",
        "boolean; unsupported-format; java.lang.Boolean.TRUE; true",
        "integer; null; java.math.BigInteger.ONE; 1",
        "integer; unsupported-format; java.math.BigInteger.ONE; 1",
        "integer; int32; 1; 1",
        "integer; int64; 1L; 1",
        "number; null; java.math.BigDecimal.ONE; 1",
        "number; unsupported-format; java.math.BigDecimal.ONE; 1",
        "number; double; 1d; 1.0",
        "number; float; 1f; 1.0",
        "string; null; \"string\"; \"string\"",
        "string; unsupportedFormat; \"string\"; \"string\"",
        "string; password; \"string\"; \"string\"",
        "string; byte; java.nio.ByteBuffer.allocate(1); [0]",
        "string; binary; java.nio.ByteBuffer.allocate(1); [0]",
        "string; date; java.time.LocalDate.of(2023, 01, 02); \"2023-01-02\"",
        "string; date-time; java.time.OffsetDateTime.MAX;"
            + " \"+999999999-12-31T23:59:59.999999999-18:00\""
      },
      delimiter = ';')
  @interface ScalarSource {}

  /**
   * Standard test template for all test classes. The implementation may assume the component is
   * named Test and the property is named Value (e.g. {{package}}.test.Value is the FQN of the value
   * type, if one is generated).
   */
  interface TestTemplate {

    /** A java expression that instantiates the value field. No return keyword, no trailing `;`. */
    String valueJava();

    /** The JSON string representation of the value property */
    String valueJson();

    /** The impl should define a setup (beforeAll, beforeEach) and use this to generate sources. */
    static void setup(String propertySchema, String otherComponents, LilyTestSupport support) {
      support.compileOas(
          """
          openapi: 3.0.2
          components:
            schemas:
              Test:
                type: object
                properties:
                  value:
          %s
          %s
          """
              .formatted(propertySchema.indent(10), otherComponents.indent(4)));
    }

    @Test
    default void hasFieldAndGetter(LilyTestSupport support) {
      assertTrue(
          support.evaluate(
              """
              var value = %s;
              return value == new {{package}}.Test(value).value();
              """
                  .formatted(valueJava()),
              Boolean.class));
    }

    @Test
    default void canSerialize(LilyTestSupport support) throws Exception {
      var obj = support.evaluate("return new {{package}}.Test(%s);".formatted(valueJava()));
      assertEquals("{\"value\":%s}".formatted(valueJson()), MAPPER.writeValueAsString(obj));
    }

    @Test
    default void canDeserialize(LilyTestSupport support) throws Exception {
      assertEquals(
          support.evaluate("return new {{package}}.Test(%s);".formatted(valueJava())),
          MAPPER.readValue(
              "{\"value\":%s}".formatted(valueJson()),
              support.getClassForName("{{package}}.Test")));
    }
  }

  /* ParameterizedTest can't use TestTemplate, so we have to live with this duplication */
  @Nested
  class WhenScalarProperties {
    @ParameterizedTest
    @ScalarSource
    @ExtendWith(LilyExtension.class)
    void hasFieldAndGetter(
        String oasType, String oasFormat, String value, String _json, LilyTestSupport support) {
      support.compileOas(
          Mustache.writeString(
              """
              openapi: 3.0.2
              components:
                schemas:
                  Foo:
                    type: object
                    properties:
                      p:
                        type: {{type}}
                        format: {{format}}
                    required: ['p']
              """,
              "scalar-parameter",
              Map.of(
                  "type", oasType,
                  "format", oasFormat)));

      assertTrue(
          support.evaluate(
              """
              var value = {{value}};
              return value == new {{package}}.Foo(value).p();
              """,
              Boolean.class,
              "value",
              value));
    }

    @ParameterizedTest
    @ScalarSource
    @ExtendWith(LilyExtension.class)
    void canSerialize(
        String oasType, String oasFormat, String value, String json, LilyTestSupport support)
        throws JsonProcessingException {
      support.compileOas(
          Mustache.writeString(
              """
              openapi: 3.0.2
              components:
                schemas:
                  Foo:
                    type: object
                    properties:
                      p:
                        type: {{type}}
                        format: {{format}}
              """,
              "scalar-parameter",
              Map.of(
                  "type", oasType,
                  "format", oasFormat)));

      var obj =
          support.evaluate(
              """
              return new {{package}}.Foo({{value}});
              """,
              "value",
              value);
      assertEquals("{\"p\":%s}".formatted(json), MAPPER.writeValueAsString(obj));
    }

    @ParameterizedTest
    @ScalarSource
    @ExtendWith(LilyExtension.class)
    void canDeserialize(
        String oasType, String oasFormat, String value, String json, LilyTestSupport support)
        throws ClassNotFoundException, JsonProcessingException {
      support.compileOas(
          Mustache.writeString(
              """
              openapi: 3.0.2
              components:
                schemas:
                  Foo:
                    type: object
                    properties:
                      p:
                        type: {{type}}
                        format: {{format}}
              """,
              "scalar-parameter",
              Map.of(
                  "type", oasType,
                  "format", oasFormat)));

      var expected =
          support.evaluate(
              """
              return new {{package}}.Foo({{value}});
              """,
              "value",
              value);

      assertEquals(
          expected,
          MAPPER.readValue(
              "{\"p\":%s}".formatted(json), support.getClassForName("{{package}}.Foo")));
    }
  }

  @Nested
  @ExtendWith(LilyExtension.class)
  class WhenInlineArrayProperties implements TestTemplate {
    @BeforeAll
    static void beforeAll(LilyTestSupport support) {
      TestTemplate.setup(
          """
          type: array
          items:
            type: integer
            format: int32
          """,
          "",
          support);
    }

    @Override
    public String valueJava() {
      return "java.util.List.of(1)";
    }

    @Override
    public String valueJson() {
      return "[1]";
    }
  }

  @Nested
  @ExtendWith(LilyExtension.class)
  class WhenInlineObjectProperties implements TestTemplate {
    @BeforeAll
    static void beforeAll(LilyTestSupport support) {
      TestTemplate.setup("type: object", "", support);
    }

    @Override
    public String valueJava() {
      return "new {{package}}.test.Value()";
    }

    @Override
    public String valueJson() {
      return "{}";
    }
  }

  @Nested
  @ExtendWith(LilyExtension.class)
  class WhenRefProperties implements TestTemplate {
    @BeforeAll
    static void beforeAll(LilyTestSupport support) {
      TestTemplate.setup(
          "$ref: '#/components/schemas/Ref'",
          """
          Ref:
            type: string
          """,
          support);
    }

    @Override
    public String valueJava() {
      return """
      new {{package}}.Ref("foo!")
      """;
    }

    @Override
    public String valueJson() {
      return "\"foo!\"";
    }
  }

  @Test
  @ExtendWith(LilyExtension.class)
  void implicitObjects(LilyTestSupport support) {
    support.compileOas(
        """
        openapi: 3.0.2
        components:
          schemas:
            MySchema:
              properties:
                value:
                  type: string
        """);

    assertTrue(
        support.evaluate(
            """
            var value = "foo!";
            return value == {{package}}.MySchema.newBuilder()
                .setValue(value)
                .buildUnvalidated()
                .getValue()
                .orElseThrow();
            """,
            Boolean.class),
        """
        If an object schema specification does not contain the type field, Lily infers that it is an
        object based on the presence of the properties field
        """);
  }

  @Test
  @ExtendWith(LilyExtension.class)
  void unsupportedType(LilyTestSupport support) {
    assertThrows(
        RuntimeException.class,
        () ->
            support.compileOas(
                """
                openapi: 3.0.3
                components:
                  schemas:
                    Foo:
                      properties:
                        foo:
                          type: unknowntype
                """));
  }
}
