package io.github.tomboyo.lily.compiler.feature;

import static io.github.tomboyo.lily.compiler.CompilerSupport.compileOas;
import static io.github.tomboyo.lily.compiler.CompilerSupport.evaluate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Lily generates classes from path parameters. */
@Nested
class ParameterSchemaGenerationTest {
  private static String packageName;

  @BeforeAll
  static void beforeAll() throws Exception {
    packageName =
        compileOas(
            """
                                openapi: 3.0.2
                                paths:
                                  /foo/{foo}/bar:
                                    parameters:
                                      - name: foo
                                        in: path
                                        required: true
                                        schema:
                                          type: object
                                          properties:
                                            foo:
                                              type: string
                                    get:
                                      operationId: GetById
                                      parameters:
                                        - name: bar
                                          in: query
                                          schema:
                                            type: object
                                            properties:
                                              bar:
                                                type: boolean
                                """);
  }

  @Test
  void pathItemParametersAreGeneratedToTypes() {
    assertThat(
        "Lily generates new types for path (item) parameter object schemas",
        "value",
        is(
            evaluate(
                """
                                        return new %s.getbyidoperation.Foo("value").foo();
                                        """
                    .formatted(packageName))));
  }

  @Test
  void operationParametersAreGeneratedToTypes() {
    assertThat(
        "Lily generates new types for operation parameter object schemas",
        true,
        is(
            evaluate(
                """
                                        return new %s.getbyidoperation.Bar(true).bar();
                                        """
                    .formatted(packageName))));
  }
}
