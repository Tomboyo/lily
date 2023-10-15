package io.github.tomboyo.lily.compiler.feature.components.schemas;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tomboyo.lily.compiler.LilyExtension;
import io.github.tomboyo.lily.compiler.LilyExtension.LilyTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** These tests cover all components.schemas elements using composition keywords. */
public class ComposedTests {

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

  /* Tests covering when a property is considered "mandatory," i.e. must be set to a non-null value in order for the
  schema to validate. Properties which are required and not-nullable are considered mandatory. Some components can
  contribute mandatory properties. */
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

    @Nested
    class FromOneOfSchema {
      @Nested
      @ExtendWith(LilyExtension.class)
      class WithoutConsensus {
        @BeforeAll
        static void beforeAll(LilyTestSupport support) {
          support.compileOas(
              """
              openapi: 3.0.3
              components:
                schemas:
                  Foo:
                    oneOf:
                      - properties: {}
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
                  Unless every OneOf component agrees that a property is mandatory, the composed schema does not
                  consider the property mandatory either without another reason to do so.
                  """);
        }
      }

      @Nested
      @ExtendWith(LilyExtension.class)
      class WithConsensus {
        @BeforeAll
        static void beforeAll(LilyTestSupport support) {
          support.compileOas(
              """
              openapi: 3.0.3
              components:
                schemas:
                  Foo:
                    oneOf:
                      -
              %s
                      -
              %s
                          # The second oneOf also has a third mandatory property, unlike the preceding component
                          mandatory3:
                            type: string
                          required: ['mandatory3']
              """
                  .formatted(propertiesFragment.indent(10), propertiesFragment.indent(10)));
        }

        @ParameterizedTest
        @CsvSource({"Mandatory1", "Mandatory2"})
        void mandatoryProperties(String name, LilyTestSupport support) {
          assertPropertyIsMandatory(
              name,
              support,
              """
                If every oneOf component considers a property to be mandatory, then so does the composed schema.
                """);
        }

        @ParameterizedTest
        @CsvSource({"Mandatory3", "Optional1", "Optional2", "Optional3"})
        void optionalProperties(String name, LilyTestSupport support) {
          assertPropertyIsOptional(
              name,
              support,
              """
                  If one or more oneOf components consider a property to be optional, then so does the composed schema
                  unless there is another reason to consider it mandatory.
                  """);
        }
      }
    }

    private static String nestedFragment =
        """
            oneOf:
              - properties:
                  a:
                    type: string
                  b:
                    type: string
                required: ['a']
              - properties:
                  a:
                    type: string
                required: ['a']
            allOf:
              - properties:
                  c:
                    type: string
                  d:
                    type: string
                required: ['c']
            anyOf:
              - properties:
                  e:
                    type: string
            """;

    @Nested
    @ExtendWith(LilyExtension.class)
    class NestedBeneathAllOf {
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
                .formatted(nestedFragment.indent(10)));
      }

      @ParameterizedTest
      @CsvSource({"A", "C"})
      void isMandatory(String name, LilyTestSupport support) {
        assertPropertyIsMandatory(
            name,
            support,
            """
                Components of allOf components may contribute mandatory properties to a top-level composed schema.
                """);
      }

      @ParameterizedTest
      @CsvSource({"B", "D", "E"})
      void isOptional(String name, LilyTestSupport support) {
        assertPropertyIsOptional(
            name,
            support,
            """
                Components of allOf components may contribute optional properties to a top-level composed schema.
                """);
      }
    }

    @Nested
    @ExtendWith(LilyExtension.class)
    class NestedBeneathAnyOf {
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
                .formatted(nestedFragment.indent(10)));
      }

      @ParameterizedTest
      @CsvSource({"A", "B", "C", "D", "E"})
      void isOptional(String name, LilyTestSupport support) {
        assertPropertyIsOptional(
            name,
            support,
            """
                Components of anyOf components can contribute optional properties to a top-level composed schema.
                """);
      }
    }

    @Nested
    @ExtendWith(LilyExtension.class)
    class NestedBeneathOneOf {
      @BeforeAll
      static void beforeAll(LilyTestSupport support) {
        support.compileOas(
            """
                    openapi: 3.0.3
                    components:
                      schemas:
                        Foo:
                          oneOf:
                            - properties:
                                a:
                                  type: string
                                c:
                                  type: string
                              required: ['a', 'c']
                            -
                    %s
                    """
                .formatted(nestedFragment.indent(10)));
      }

      @ParameterizedTest
      @CsvSource({"A", "C"})
      void mandatoryProperties(String name, LilyTestSupport support) {
        assertPropertyIsMandatory(
            name,
            support,
            """
                Components of OneOf components may contribute mandatory properties to top-level composed schema.
                """);
      }

      @ParameterizedTest
      @CsvSource({"B", "D", "E"})
      void optionalProperties(String name, LilyTestSupport support) {
        assertPropertyIsOptional(
            name,
            support,
            """
                Components of OneOf components may contribute optional properties to top-level composed schema.
                """);
      }
    }
  }

  /* Tests covering inheritance/scope rules for the required keyword. Whether a property is required on a composed
  schema may be governed by whether it is required in component schema. */
  @Nested
  class RequiredKeywordScope {

    static final String template =
        """
            openapi: 3.0.3
            components:
              schemas:
                Foo:
                  properties:
                    a:
                      type: string
                  %s:
                    - required: ['a']
            """;

    @Test
    @ExtendWith(LilyExtension.class)
    void testAllOf(LilyTestSupport support) {
      support.compileOas(template.formatted("allOf"));
      assertPropertyIsMandatory(
          "A",
          support,
          """
              If an allOf component declares a property required, then the composed schema treats the property as required as
              well. Otherwise optional properties on the composed schema may be mandatory as a result.
              """);
    }

    @Test
    @ExtendWith(LilyExtension.class)
    void testAnyOf(LilyTestSupport support) {
      support.compileOas(template.formatted("anyOf"));
      assertPropertyIsOptional(
          "A",
          support,
          """
              Because anyOf components are inherently optional, they cannot cause properties of the composed schema to
              be required via the required keyword.
              """);
    }

    @Nested
    @ExtendWith(LilyExtension.class)
    class OneOfComponent {
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
                        b:
                          type: string
                        c:
                          type: string
                      oneOf:
                        - required: ['a']
                        - required: ['a', 'b']
                        - required: ['c', 'a']
                """);
      }

      @Test
      void withConsensus(LilyTestSupport support) {
        assertPropertyIsMandatory(
            "A",
            support,
            """
                If every oneOf component agrees a property is required, then the composed schema considers the property
                required as well, potentially making an otherwise optional property mandatory.
                """);
      }

      @ParameterizedTest
      @CsvSource({"B", "C"})
      void withoutConsensus(String name, LilyTestSupport support) {
        assertPropertyIsOptional(
            name,
            support,
            """
                If oneOf components do not agree that a property is required, then they cannot cause the composed schema
                to consider the property required by themselves. As a result, disagreeing oneOf components couldn't
                cause a property to become mandatory on the composed schema.
                """);
      }
    }

    @Nested
    class WhenNested {
      static final String template =
          """
              openapi: 3.0.3
              components:
                schemas:
                  Foo:
                    properties:
                      a:
                        type: string
                      b:
                        type: string
                      c:
                        type: string
                    %s:
                      - allOf:
                        - required: ['a', 'c']
                      - oneOf:
                        - required: ['b', 'c']
              """;

      @Nested
      @ExtendWith(LilyExtension.class)
      class NestedBeneathAllOf {
        @BeforeAll
        static void beforeAll(LilyTestSupport support) {
          support.compileOas(template.formatted("allOf"));
        }

        @ParameterizedTest
        @CsvSource({"A", "B", "C"})
        void isRequired(String name, LilyTestSupport support) {
          assertPropertyIsMandatory(
              name,
              support,
              """
                          Required properties may be inherited through components nested beneath an allOf component.
                          """);
        }
      }

      @Nested
      @ExtendWith(LilyExtension.class)
      class NestedBeneathAnyOf {
        @BeforeAll
        static void beforeAll(LilyTestSupport support) {
          support.compileOas(template.formatted("anyOf"));
        }

        @ParameterizedTest
        @CsvSource({"A", "B", "C"})
        void isOptional(String name, LilyTestSupport support) {
          assertPropertyIsOptional(
              name,
              support,
              """
                  Required properties are never inherited through anyOf components, which are inherently optional.
                  """);
        }
      }

      @Nested
      @ExtendWith(LilyExtension.class)
      class NestedBeneathOneOf {
        @BeforeAll
        static void beforeAll(LilyTestSupport support) {
          support.compileOas(template.formatted("oneOf"));
        }

        @Test
        void isMandatory(LilyTestSupport support) {
          assertPropertyIsMandatory(
              "C",
              support,
              """
                  Required properties may be inherited through components nested beneath an oneOf component. All nested
                  oneOf components must agree that a property is required, however.
                  """);
        }

        @ParameterizedTest
        @CsvSource({"A", "B"})
        void isOptional(String name, LilyTestSupport support) {
          assertPropertyIsOptional(
              name,
              support,
              """
                  If components nested beneath a oneOf component do not agree that a property is required, then unless
                  the top-level composed schema has another reason to consider the property required, it is not.
                  """);
        }
      }
    }
  }
}
