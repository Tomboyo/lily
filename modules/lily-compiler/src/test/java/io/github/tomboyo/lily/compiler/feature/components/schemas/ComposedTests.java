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

    /* This fragment declares two mandatory properties and three optional ones using evey combination of required and
    nullable attribute. Note that whether the property is considered mandatory by a composed schema depends on where
    this fragment is embedded. If embedded into an anyOf schema, for example, it could be that while the component
    considers these properties mandatory, the composed schema des not. */
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

    void assertPropertyIsMandatory(String name, LilyTestSupport support, String message) {
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
          message);
    }

    @Nested
    @ExtendWith(LilyExtension.class)
    class FromPropertiesKeyword {
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
                .formatted(propertiesFragment.indent(6)));
      }

      @ParameterizedTest
      @CsvSource({"Mandatory1", "Mandatory2"})
      void mandatoryProperties(String name, LilyTestSupport support) {
        assertPropertyIsMandatory(
            name,
            support,
            """
                Properties from the properties keyword of a schema which are required and non-nullable are mandatory.
                For each such property, Foo's builder must define a setter and Foo must define a non-Optional typed
                getter.
                """);
      }

      @ParameterizedTest
      @CsvSource({"Optional1", "Optional2", "Optional3"})
      void optionalProperties(String name, LilyTestSupport support) {
        assertPropertyIsOptional(
            name,
            support,
            """
                Properties from the properties keyword of a schema which are either not required or are nullable are not
                mandatory (i.e. optional). For each such property, Foo's builder must define a setter and Foo must
                define an Optional-typed getter.
                """);
      }
    }

    @Nested
    @ExtendWith(LilyExtension.class)
    class FromAllOfSchema {
      @BeforeAll
      static void beforeAll(LilyTestSupport support) {
        support.compileOas(
            """
                openapi: 3.0.3
                components:
                  schemas:
                    Foo:
                      allOf:
                        -
                %s
                """
                .formatted(propertiesFragment.indent(10)));
      }

      @ParameterizedTest
      @CsvSource({"Mandatory1", "Mandatory2"})
      void mandatoryProperties(String name, LilyTestSupport support) {
        assertPropertyIsMandatory(
            name,
            support,
            """
                If a property is considered mandatory by a component allOf schema, then it is mandatory according to the
                composed schema as well.
                """);
      }

      @ParameterizedTest
      @CsvSource({"Optional1", "Optional2", "Optional3"})
      void optionalProperties(String name, LilyTestSupport support) {
        assertPropertyIsOptional(
            name,
            support,
            """
                If a property is considered optional by a component allOf schema, and there are no other reasons that a
                property should be mandatory, the property is considered optional by the composed schema as well.
                """);
      }
    }

    /* Properties which are mandatory according to an allOf component are mandatory according to the composed schema as
    well. As a result, mandatory properties from nested allOf components are "transitively" mandatory. */
    @Nested
    @ExtendWith(LilyExtension.class)
    class FromNestedAllOfSchema {
      @BeforeAll
      static void beforeAll(LilyTestSupport support) {
        support.compileOas(
            """
                openapi: 3.0.3
                components:
                  schemas:
                    Foo:
                      allOf:
                        - allOf:
                          - allOf:
                %s
                """
                .formatted(propertiesFragment.indent(12)));
      }

      @ParameterizedTest
      @CsvSource({"Mandatory1", "Mandatory2"})
      void mandatoryProperties(String name, LilyTestSupport support) {
        assertPropertyIsMandatory(
            name,
            support,
            """
                If a property is mandatory according to an allOf schema, then it is considered mandatory by the composed
                schemas as well, even transitively though nested allOf schemas.
                """);
      }

      @ParameterizedTest
      @CsvSource({"Optional1", "Optional2", "Optional3"})
      void optionalProperties(String name, LilyTestSupport support) {
        assertPropertyIsOptional(
            name,
            support,
            """
                If a property is considered optional by a component allOf schema, and there are no other reasons that a
                property should be mandatory, the property is considered optional by the composed schema as well,
                including if the property is defined transitively through nested allOf schemas.
                """);
      }
    }

    @Nested
    @ExtendWith(LilyExtension.class)
    class FromAnyOfSchema {
      @BeforeAll
      static void beforeAll(LilyTestSupport support) {
        support.compileOas(
            """
            openapi: 3.0.3
            components:
              schemas:
                Foo:
                  anyOf:
                    -
            %s
            """
                .formatted(propertiesFragment.indent(10)));
      }

      @ParameterizedTest
      @CsvSource({"Mandatory1", "Mandatory2", "Optional1", "Optional2", "Optional3"})
      void allPropertiesAreOptional(String name, LilyTestSupport support) {
        assertPropertyIsOptional(
            name,
            support,
            """
            Because an object is never obligated to validate against an anyOf schema, mandatory properties from
            anyOf component schemas are not mandatory in the composed schema unless there is another reason for them
            to be mandatory.
            """);
      }
    }
  }
}
