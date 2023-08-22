package io.github.tomboyo.lily.compiler.icg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.tomboyo.lily.compiler.LilyExtension;
import io.github.tomboyo.lily.compiler.LilyExtension.LilyTestSupport;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.cg.Mustache;
import io.swagger.v3.oas.models.media.ObjectSchema;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class OasComponentsToAstTest {

  @Nested
  class ScalarComponents {

    @RegisterExtension
    static LilyExtension extension = LilyExtension.newBuilder().packagePerMethod().build();

    @ParameterizedTest
    @CsvSource({
      "boolean, null, java.lang.Boolean.TRUE",
      "boolean, unsupported-format, java.lang.Boolean.TRUE",
      "integer, null, java.math.BigInteger.ONE",
      "integer, unsupported-format, java.math.BigInteger.ONE",
      "integer, int32, 1", // integer
      "integer, int64, 1L", // long
      "number, null, java.math.BigDecimal.ONE",
      "number, unsupported-format, java.math.BigDecimal.ONE",
      "number, double, 1d", // double
      "number, float, 1f", // float
      "string, null, \"string\"",
      "string, unsupportedFormat, \"string\"",
      "string, password, \"string\"",
      "string, byte, java.nio.ByteBuffer.allocate(1)",
      "string, binary, java.nio.ByteBuffer.allocate(1)",
      "string, date, java.time.LocalDate.now()",
      "string, date-time, java.time.OffsetDateTime.now()"
    })
    void test(String oasType, String oasFormat, String value, LilyTestSupport support) {
      support.compileOas(
          Mustache.writeString(
              """
              openapi: 3.0.2
              components:
                schemas:
                  Foo:
                    properties:
                      p:
                        type: {{type}}
                        format: {{format}}
              """,
              "scalar-components-test",
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
  }

  @Nested
  @ExtendWith(LilyExtension.class)
  class Refs {

    @BeforeAll
    static void beforeAll(LilyTestSupport support) {
      support.compileOas(
          """
              openapi: 3.0.2
              components:
                schemas:
                  MyComponent:
                    $ref: '#/components/schemas/Foo'
                  Foo:
                    properties:
                      foo:
                        type: string
              """);
    }

    @Test
    void testValue(LilyTestSupport support) {
      assertTrue(
          support.evaluate(
              """
                var foo = new {{package}}.Foo("foo!");
                return foo == new {{package}}.MyComponent(foo).value();
                """,
              Boolean.class),
          "Schemas that alias a $ref become alias types");
    }

    @Test
    void testJson(LilyTestSupport support) throws JsonProcessingException {
      var alias =
          support.evaluate(
              """
              var foo = new {{package}}.Foo("foo!");
              return new {{package}}.MyComponent(foo);
              """);

      var mapper = new ObjectMapper();
      assertEquals(
          "{\"foo\":\"foo!\"}",
          mapper.writeValueAsString(alias),
          "Alias are serialized as if they were only their aliased value");
    }
  }

  @Nested
  class Arrays {

    @Nested
    @ExtendWith(LilyExtension.class)
    class WithScalarItem {

      @BeforeAll
      static void beforeAll(LilyTestSupport support) {
        support.compileOas(
            """
              openapi: 3.0.2
              components:
                schemas:
                  MyComponent:
                    type: array
                    items:
                      type: boolean
              """);
      }

      @Test
      void testAliasing(LilyTestSupport support) {
        assertTrue(
            support.evaluate(
                """
                var value = java.util.List.of(true, false);
                return value == new {{package}}.MyComponent(value).value();
                """,
                Boolean.class));
      }

      @Test
      void testJson(LilyTestSupport support) throws JsonProcessingException {
        var right =
            support.evaluate(
                """
                return new {{package}}.MyComponent(java.util.List.of(true, false));
                """);

        var mapper = new ObjectMapper();
        assertEquals(
            "[true,false]",
            mapper.writeValueAsString(right),
            "Arrays types serialize to bare arrays");
      }
    }

    @Nested
    @ExtendWith(LilyExtension.class)
    class WithImplicitObjectItem {

      @BeforeAll
      static void beforeAll(LilyTestSupport support) {
        support.compileOas(
            """
                openapi: 3.0.2
                components:
                  schemas:
                    MyComponent:
                      type: array
                      items:
                        properties:
                          foo:
                            type: string
                """);
      }

      @Test
      void testValue(LilyTestSupport support) {
        assertTrue(
            support.evaluate(
                """
                var value = java.util.List.of(new {{package}}.mycomponent.MyComponentItem("foo!"));
                return value == new {{package}}.MyComponent(value).value();
                """,
                Boolean.class));
      }

      @Test
      void testSer(LilyTestSupport support) throws JsonProcessingException {
        var value =
            support.evaluate(
                """
                var value = java.util.List.of(new {{package}}.mycomponent.MyComponentItem("foo!"));
                return new {{package}}.MyComponent(value);
                """);
        var mapper = new ObjectMapper();
        assertEquals("[{\"foo\":\"foo!\"}]", mapper.writeValueAsString(value));
      }

      @Test
      void testDeser(LilyTestSupport support)
          throws ClassNotFoundException, JsonProcessingException {
        var expected =
            support.evaluate(
                """
                var value = java.util.List.of(new {{package}}.mycomponent.MyComponentItem("foo!"));
                return new {{package}}.MyComponent(value);
                """);
        var mapper = new ObjectMapper();
        assertEquals(
            expected,
            mapper.readValue(
                "[{\"foo\":\"foo!\"}]", support.getClassForName("{{package}}.MyComponent")));
      }
    }

    @Nested
    @ExtendWith(LilyExtension.class)
    class WithExplicitObjectItem {

      @BeforeAll
      static void beforeAll(LilyTestSupport support) {
        support.compileOas(
            """
                openapi: 3.0.2
                components:
                  schemas:
                    MyComponent:
                      type: array
                      items:
                        # type is explicitly given (not inferred from presence of properties key)
                        type: object
                        properties:
                          foo:
                            type: string
                """);
      }

      @Test
      void testValue(LilyTestSupport support) {
        assertTrue(
            support.evaluate(
                """
                  var value = java.util.List.of(new {{package}}.mycomponent.MyComponentItem("foo!"));
                  return value == new {{package}}.MyComponent(value).value();
                  """,
                Boolean.class),
            """
                The anonymous object type is generated to a subordinate package. The list is accessible through the
                value method.
                """);
      }

      @Test
      void testJsonSer(LilyTestSupport support) throws JsonProcessingException {
        var actual =
            support.evaluate(
                """
                        var value = java.util.List.of(new {{package}}.mycomponent.MyComponentItem("foo!"));
                        return new {{package}}.MyComponent(value);
                        """);
        var mapper = new ObjectMapper();
        assertEquals("[{\"foo\":\"foo!\"}]", mapper.writeValueAsString(actual));
      }

      @Test
      void testJsonDeser(LilyTestSupport support)
          throws JsonProcessingException, ClassNotFoundException {
        var expected =
            support.evaluate(
                """
                        var value = java.util.List.of(new {{package}}.mycomponent.MyComponentItem("foo!"));
                        return new {{package}}.MyComponent(value);
                        """);
        var mapper = new ObjectMapper();
        assertEquals(
            expected,
            mapper.readValue(
                "[{\"foo\":\"foo!\"}]", support.getClassForName("{{package}}.MyComponent")));
      }
    }

    //  TODO: do we need to test implicit/explicit here? I think just recusing once should be enough
    // right?
    @Nested
    @ExtendWith(LilyExtension.class)
    class WithArrayOfExplicitObjectItem {
      @BeforeAll
      static void beforeAll(LilyTestSupport support) {
        support.compileOas(
            """
                openapi: 3.0.2
                components:
                  schemas:
                    MyComponent:
                      type: array
                      items:
                        type: array
                        items:
                          type: object
                          properties:
                            foo:
                              type: string
                """);
      }

      @Test
      void testValue(LilyTestSupport support) {
        assertTrue(
            support.evaluate(
                """
                var value = java.util.List.of(java.util.List.of(new {{package}}.mycomponent.MyComponentItem("foo!")));
                return value == new {{package}}.MyComponent(value).value();
                """,
                Boolean.class));
      }

      @Test
      void testSer(LilyTestSupport support) throws JsonProcessingException {
        var value =
            support.evaluate(
                """
                var value = java.util.List.of(java.util.List.of(new {{package}}.mycomponent.MyComponentItem("foo!")));
                return new {{package}}.MyComponent(value).value();
                """);
        var mapper = new ObjectMapper();
        assertEquals("[[{\"foo\":\"foo!\"}]]", mapper.writeValueAsString(value));
      }

      @Test
      void testDeser(LilyTestSupport support)
          throws ClassNotFoundException, JsonProcessingException {
        var expected =
            support.evaluate(
                """
                var value = java.util.List.of(java.util.List.of(new {{package}}.mycomponent.MyComponentItem("foo!")));
                return new {{package}}.MyComponent(value);
                """);
        var mapper = new ObjectMapper();
        assertEquals(
            expected,
            mapper.readValue(
                "[[{\"foo\":\"foo!\"}]]", support.getClassForName("{{package}}.MyComponent")));
      }
    }

    @Nested
    @ExtendWith(LilyExtension.class)
    class RefItem {
      @BeforeAll
      static void beforeAll(LilyTestSupport support) {
        support.compileOas(
            """
                openapi: 3.0.2
                components:
                  schemas:
                    MyComponent:
                      type: array
                      items:
                        $ref: '#/components/schemas/Foo'
                    Foo:
                      properties:
                        foo:
                          type: string
                """);
      }

      @Test
      void testValue(LilyTestSupport support) {
        assertTrue(
            support.evaluate(
                """
                var value = java.util.List.of(new {{package}}.Foo("foo!");
                return value == new {{package}}.MyComponent(value).value();
                """,
                Boolean.class));
      }

      @Test
      void testSer(LilyTestSupport support) throws JsonProcessingException {
        var value =
            support.evaluate(
                """
                var value = java.util.List.of(new {{package}}.Foo("foo!"));
                return new {{package}}.MyComponent(value);
                """);
        var mapper = new ObjectMapper();
        assertEquals("[{\"foo\":\"foo!\"}]", mapper.writeValueAsString(value));
      }

      @Test
      void testDeser(LilyTestSupport support)
          throws ClassNotFoundException, JsonProcessingException {
        var expected =
            support.evaluate(
                """
                var value = java.util.List.of(new {{package}}.Foo("foo!"));
                return new {{package}}.MyComponent(value);
                """);
        var mapper = new ObjectMapper();
        assertEquals(
            expected,
            mapper.readValue(
                "[{\"foo\":\"foo!\"}]", support.getClassForName("{{package}}.MyComponent")));
      }
    }
  }

  @Nested
  class Objects {
    @Test
    void evaluate() {
      assertEquals(
          OasSchemaToAst.evaluate(
                  PackageName.of("p"), SimpleName.of("MyComponent"), new ObjectSchema())
              .right()
              .collect(Collectors.toSet()),
          OasComponentsToAst.evaluate(
                  PackageName.of("p"), SimpleName.of("MyComponent"), new ObjectSchema())
              .collect(Collectors.toSet()),
          "Object components are evaluated the same as any other object schema (i.e. not aliased)");
    }
  }
}
