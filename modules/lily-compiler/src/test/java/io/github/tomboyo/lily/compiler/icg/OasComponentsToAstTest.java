package io.github.tomboyo.lily.compiler.icg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.tomboyo.lily.compiler.LilyExtension;
import io.github.tomboyo.lily.compiler.LilyExtension.LilyTestSupport;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.swagger.v3.oas.models.media.ObjectSchema;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

public class OasComponentsToAstTest {

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
                var value = java.util.List.of(new {{package}}.Foo("foo!"));
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
