package io.github.tomboyo.lily.compiler.feature.components.schemas;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

import io.github.tomboyo.lily.compiler.LilyExtension;
import io.github.tomboyo.lily.compiler.LilyExtension.LilyTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** These tests cover all components.schemas elements using composition keywords. */
public class ComposedTests {

  @Nested
  class MandatoryProperties {
    static final String propertiesFragment =
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

    static void assertPropertyIsOptional(String name, LilyTestSupport support, String message) {
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
              name),
          message);
    }

    /* Properties from the properties keyword and composed through the allOf keyword behave the same. This test template
    covers them. */
    @TestInstance(PER_CLASS)
    @ExtendWith(LilyExtension.class)
    abstract class TestTemplate {
      abstract String schemaFragment();

      @BeforeAll
      void beforeAll(LilyTestSupport support) {
        support.compileOas(
            """
                  openapi: 3.0.3
                  components:
                    schemas:
                      Foo:
                  %s
                  """
                .formatted(schemaFragment().indent(6)));
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

    @Nested
    class FromPropertiesKeyword extends TestTemplate {
      @Override
      String schemaFragment() {
        return propertiesFragment;
      }
    }

    @Nested
    class FromAllOfSchema extends TestTemplate {
      @Override
      String schemaFragment() {
        return """
                allOf:
                -
                %s
                """
            .formatted(propertiesFragment.indent(2));
      }
    }

    /* Properties which are mandatory according to an allOf component are mandatory according to the composed schema as
    well. As a result, mandatory properties from nested allOf components are "transitively" mandatory. */
    @Nested
    class FromNestedAllOfSchema extends TestTemplate {
      @Override
      String schemaFragment() {
        return """
               allOf:
               - allOf:
                 - allOf:
                   -
               %s
               """
            .formatted(propertiesFragment.indent(6));
      }
    }

    /* Properties from the AnyOf and OneOf keywords behave differently than from allOf and properties keywords. These do
    not use the test template. */
    @Nested
    @ExtendWith(LilyExtension.class)
    class FromAnyAndOneOfSchema {

      @Nested
      class WithoutConsensus {
        @BeforeAll
        static void beforeAll(LilyTestSupport support) {
          support.compileOas(
              """
                  openapi: 3.0.3
                  components:
                    schemas:
                      Foo:
                        anyOf:
                        - properties: {}
                        -
                  %s
                  """
                  .formatted(propertiesFragment.indent(8)));
        }

        @ParameterizedTest
        @CsvSource({"Mandatory1", "Mandatory2", "Optional1", "Optional2", "Optional3"})
        void allPropertiesAreOptional(String name, LilyTestSupport support) {
          assertPropertyIsOptional(
              name,
              support,
              """
              Unless every anyOf and oneOf component agree that a property is mandatory, it is not mandatory on the
              composed schema. Foo's builder must define a setter for each such property, and Foo must define an
              Optional-typed getter for each such property.
              """);
        }
      }
    }
  }
}
