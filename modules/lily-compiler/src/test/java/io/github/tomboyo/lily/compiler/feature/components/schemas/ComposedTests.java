package io.github.tomboyo.lily.compiler.feature.components.schemas;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tomboyo.lily.compiler.LilyExtension;
import io.github.tomboyo.lily.compiler.LilyExtension.LilyTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** These tests cover all components.schemas elements using composition keywords. */
public class ComposedTests {

  @Nested
  @ExtendWith(LilyExtension.class)
  class MandatoryProperties {
    @BeforeAll
    static void beforeAll(LilyTestSupport support) {
      support.compileOas(
          """
              openapi: 3.0.3
              components:
                schemas:
                  Foo:
                    properties:
                      a:
                        type: string
                        nullable: true
                      b:
                        type: string
                      c:
                        type: string
                      d:
                        type: string
                        nullable: false
                    required: ['a', 'c', 'd']
              """);
    }

    @ParameterizedTest
    @CsvSource({"c", "d"})
    void mandatoryProperties(String name, LilyTestSupport support) {
      assertTrue(
          support.evaluate(
              """
              var value = "foo!";
              return value == {{package}}.Foo.newBuilder()
                  .set{{Name}}(value)
                  .build()
                  .get{{Name}}();
              """,
              Boolean.class,
              "Name",
              name.toUpperCase()));
    }

    @ParameterizedTest
    @CsvSource({"a", "b"})
    void nonMandatoryProperties(String name, LilyTestSupport support) {
      assertTrue(
          support.evaluate(
              """
              var value = "foo!";
              java.util.Optional<String> actual = {{package}}.Foo.newBuilder()
                  .set{{Name}}(value)
                  .build()
                  .get{{Name}}();
              return value == actual.orElseThrow();
              """,
              Boolean.class,
              "Name",
              name.toUpperCase()));
    }
  }
}
