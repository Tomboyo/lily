package io.github.tomboyo.lily.compiler.feature;

import io.github.tomboyo.lily.compiler.LilyExtension;
import io.github.tomboyo.lily.compiler.LilyExtension.LilyTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

public class ComponentSchemaTest {

  @Nested
  @ExtendWith(LilyExtension.class)
  class WhenObjectSchema {
    @BeforeAll
    static void beforeAll(LilyTestSupport support) {
      support.compileOas(
          """
          openapi: 3.0.2
          components:
            schemas:
              MySchema:
                type: object
                properties:
                  foo:
                    type: integer
                    format: int32
          """);
    }

    @Test
    void test(LilyTestSupport support) {
      Assertions.assertDoesNotThrow(
          () ->
              support.evaluate(
                  """
            return new {{package}}.MySchema(1);
            """));
    }
  }

  @Nested
  @ExtendWith(LilyExtension.class)
  class WhenObjectSchemaWithoutType {
    @BeforeAll
    static void beforeAll(LilyTestSupport support) {
      support.compileOas(
          """
              openapi: 3.0.2
              components:
                schemas:
                  MySchema:
                    properties:
                      foo:
                        type: string
              """);
    }

    @Test
    void test(LilyTestSupport support) {
      Assertions.assertDoesNotThrow(
          () ->
              support.evaluate(
                  """
            return new {{package}}.MySchema("myFoo");
            """),
          """
         If an object schema specification does not contain the type field, Lily infers that it is an object based on
         the presence of the properties field
         """);
    }
  }

  @Nested
  @ExtendWith(LilyExtension.class)
  class WhenComposedSchema {
    @BeforeAll
    static void beforeAll(LilyTestSupport support) {
      support.compileOas(
          """
          openapi: 3.0.2
          components:
            schemas:
              MyAllOfSchema:
                allOf:
                  - type: object
                    properties:
                      foo:
                        type: string
              MyAnyOfSchema:
                anyOf:
                  - type: object
                    properties:
                      foo:
                        type: string
              MyOneOfSchema:
                oneOf:
                  - type: object
                    properties:
                      foo:
                        type: string
              MyNotSchema:
                not:
                  type: object
                  properties:
                    foo:
                      type: string
              """);
    }

    @Test
    void test(LilyTestSupport support) {
      Assertions.assertDoesNotThrow(
          () ->
              support.evaluate(
                  """
                  new {{package}}.MyAllOfSchema();
                  new {{package}}.MyAnyOfSchema();
                  new {{package}}.MyOneOfSchema();
                  new {{package}}.MyNotSchema();

                  return null;
                  """),
          """
                   If an object schema specification contains compositional keywords allOf, anyOf, oneOf, or not fields, Lily does not support
                   these fields, but should generate a class so that the code compiles.
                """);
    }
  }
}
