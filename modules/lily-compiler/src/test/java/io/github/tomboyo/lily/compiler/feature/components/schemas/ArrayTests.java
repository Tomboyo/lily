package io.github.tomboyo.lily.compiler.feature.components.schemas;

import static io.github.tomboyo.lily.compiler.feature.components.schemas.TestSupport.MAPPER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.tomboyo.lily.compiler.LilyExtension;
import io.github.tomboyo.lily.compiler.LilyExtension.LilyTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/*
 * These tests cover all components.schemas elements of type array. These schema are generated into "alias" types,
 * specifically aliases of Lists.
 */
@Nested
public class ArrayTests {
  @Nested
  @ExtendWith(LilyExtension.class)
  class WithScalarItems {
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
    void testIsAlias(LilyTestSupport support) {
      assertTrue(
          support.evaluate(
              """
                                    var value = java.util.List.of(true, false);
                                    return value == new {{package}}.MyComponent(value).value();
                                    """,
              Boolean.class));
    }

    @Test
    void testSerialization(LilyTestSupport support) throws JsonProcessingException {
      var right =
          support.evaluate(
              """
                            return new {{package}}.MyComponent(java.util.List.of(true, false));
                            """);

      var mapper = new ObjectMapper();
      assertEquals("[true,false]", mapper.writeValueAsString(right));
    }

    @Test
    void testDeserialization(LilyTestSupport support) throws Exception {
      assertEquals(
          support.evaluate(
              """
                            return new {{package}}.MyComponent(java.util.List.of(true, false));
                            """),
          MAPPER.readValue("[true,false]", support.getClassForName("{{package}}.MyComponent")));
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
    void testIsAlias(LilyTestSupport support) {
      // Note that the anonymous object is generated to a subordinate package. It is named after the
      // parent
      // schema, but with the "Item" suffix.
      assertTrue(
          support.evaluate(
              """
                                    var value = java.util.List.of(new {{package}}.mycomponent.MyComponentItem("foo!"));
                                    return value == new {{package}}.MyComponent(value).value();
                                    """,
              Boolean.class));
    }

    @Test
    void canSerialize(LilyTestSupport support) throws JsonProcessingException {
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
    void canDeserialize(LilyTestSupport support)
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
                            # `type: object` is implied by the properties key. It is omitted.
                            properties:
                              foo:
                                type: string
                    """);
    }

    @Test
    void testIsAlias(LilyTestSupport support) {
      // Note that the anonymous object is generated to a subordinate package. It is named after the
      // parent
      // schema, but with the "Item" suffix.
      assertTrue(
          support.evaluate(
              """
                                    var value = java.util.List.of(new {{package}}.mycomponent.MyComponentItem("foo!"));
                                    return value == new {{package}}.MyComponent(value).value();
                                    """,
              Boolean.class));
    }

    @Test
    void canSerialize(LilyTestSupport support) throws JsonProcessingException {
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
    void canDeserialize(LilyTestSupport support)
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
    void isAlias(LilyTestSupport support) {
      assertTrue(
          support.evaluate(
              """
                                    var value = java.util.List.of(java.util.List.of(new {{package}}.mycomponent.MyComponentItem("foo!")));
                                    return value == new {{package}}.MyComponent(value).value();
                                    """,
              Boolean.class));
    }

    @Test
    void canSerialize(LilyTestSupport support) throws JsonProcessingException {
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
    void canDeserialize(LilyTestSupport support)
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
    void isAlias(LilyTestSupport support) {
      assertTrue(
          support.evaluate(
              """
                                    var value = java.util.List.of(new {{package}}.Foo("foo!"));
                                    return value == new {{package}}.MyComponent(value).value();
                                    """,
              Boolean.class));
    }

    @Test
    void canSerialize(LilyTestSupport support) throws JsonProcessingException {
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
    void canDeserialize(LilyTestSupport support)
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
