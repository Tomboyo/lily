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
  class MandatoryProperties {
    static final String fragment =
        """
        required: ['mandatory1', 'mandatory2', 'optional3']
        properties:
          # required and implicitly non-nullable
          mandatory1:
            type: string
          # required and explicitly non-nullable
          mandatory2:
            type: string
            nullable: false
          # Not required (and implicitly non-nullable)
          optional1:
            type: string
          # Not required (and explicitly non-nullable)
          optional2:
            type: string
            nullable: false
          # Nullable (and required)
          optional3:
            type: string
            nullable: true
        """;

    @Nested
    @ExtendWith(LilyExtension.class)
    class PropertiesKeyword {
      @BeforeAll
      static void beforeAll(LilyTestSupport support) {
        support.compileOas(
            """
              openapi: 3.0.3
              components:
                schemas:
                  Foo:
              %s
              """
                .formatted(fragment.indent(6)));
      }

      @ParameterizedTest
      @CsvSource({"Mandatory1", "Mandatory2"})
      void mandatoryProperties(String name, LilyTestSupport support) {
        assertTrue(
            support.evaluate(
                """
                var foo = "foo!";
                return {{package}}.Foo.newBuilder()
                    .set{{Name}}(foo)
                    .build()
                    .get{{Name}}() == foo;
                """,
                Boolean.class,
                "Name",
                name),
            """
            Foo's builder must define a setter function, and mandatory properties must has an associated
            non-Optional typed getter.
            """);
      }

      @ParameterizedTest
      @CsvSource({"Optional1", "Optional2", "Optional3"})
      void optionalProperties(String name, LilyTestSupport support) {
        assertTrue(
            support.evaluate(
                """
                var foo = "foo!";
                java.util.Optional<String> v = {{package}}.Foo.newBuilder()
                    .set{{Name}}(foo)
                    .build()
                    .get{{Name}}();
                return foo == v.orElseThrow();
                """,
                Boolean.class,
                "Name",
                name));
      }
    }
  }
}
