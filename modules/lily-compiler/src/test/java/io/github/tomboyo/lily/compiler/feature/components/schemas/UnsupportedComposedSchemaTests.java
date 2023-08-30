package io.github.tomboyo.lily.compiler.feature.components.schemas;

import io.github.tomboyo.lily.compiler.LilyExtension;
import io.github.tomboyo.lily.compiler.LilyExtension.LilyTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(LilyExtension.class)
public class UnsupportedComposedSchemaTests {
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
              MyNotSchema:
                not:
                  type: object
                  properties:
                    foo:
                      type: string
              """);
  }

  @Test
  void testThatClassesExist(LilyTestSupport support) {
    Assertions.assertDoesNotThrow(
        () ->
            support.evaluate(
                """
                  new {{package}}.MyAllOfSchema();
                  new {{package}}.MyAnyOfSchema();
                  new {{package}}.MyNotSchema();

                  return null;
                  """),
        """
           If an object schema specification contains compositional keywords allOf, anyOf, or not fields, Lily
           should generate a class so that the code compiles even though it doesn't implement these keywords.
           """);
  }

  @Test
  void testThatJavadocContainsWarning(LilyTestSupport support) {
    var string = support.getFileStringForClass("{{package}}.MyAllOfSchema");
    var containsString =
        string.contains(
            "Generated empty class because compositional keywords *allOf, anyOf, and not*"
                + " are not yet supported");
    Assertions.assertTrue(containsString, "Lily warns users when it generated empty classes.");
  }
}
