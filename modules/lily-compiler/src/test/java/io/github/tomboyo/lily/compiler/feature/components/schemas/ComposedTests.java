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
  class Getters {
    @BeforeAll
    static void beforeAll(LilyTestSupport support) {
      support.compileOas(
          """
                    components:
                      schemas:
                        Foo:
                          properties:
                            - a:
                                type: string
                                required: true
                            - b:
                                type: string
                          allOf:
                            - properties:
                                c:
                                  type: string
                                  required: true
                            - properties:
                                d:
                                  type: string
                          anyOf:
                            - properties:
                                e:
                                  type: string
                                  required: true
                            - properties:
                                f:
                                  type: string
                          oneOf:
                            - properties:
                                g:
                                  type: string
                                  required: true
                            - properties:
                                h:
                                  type: string
                    """);
    }

    @ParameterizedTest
    @CsvSource({"a", "c", "e", "g"})
    void requiredGetters(String parameter, LilyTestSupport support) {
      assertTrue(
          support.evaluate(
              """
                          var value = "value";
                          var foo = {{package}}.Foo.newBuilder()
                            .set{{name}}(value)
                            .build();
                          return value == foo.get{{name}}();
                          """,
              boolean.class,
              "name",
              parameter));
    }
  }
}
