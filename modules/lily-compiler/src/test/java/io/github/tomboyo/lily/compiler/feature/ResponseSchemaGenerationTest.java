package io.github.tomboyo.lily.compiler.feature;

import static io.github.tomboyo.lily.compiler.CompilerSupport.compileOas;
import static io.github.tomboyo.lily.compiler.CompilerSupport.deleteGeneratedSources;
import static io.github.tomboyo.lily.compiler.CompilerSupport.evaluate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Lily generates classes from response schema. */
@Nested
class ResponseSchemaGenerationTest {
  private static String packageName;

  @AfterAll
  static void afterAll() throws Exception {
    deleteGeneratedSources();
  }

  @BeforeAll
  static void beforeAll() throws Exception {
    packageName =
        compileOas(
            """
            openapi: 3.0.2
            paths:
              /foo:
                get:
                  operationId: GetFoo
                  responses:
                    '200':
                      content:
                        application/json:
                          schema:
                            type: object
                            properties:
                              foo:
                                type: string
            """);
  }

  @Test
  void responseSchemaAreGeneratedToTypes() {
    assertThat(
        "Lily generates new types for response schemas",
        "value",
        is(
            evaluate(
                """
                return new %s.getfoooperation.Response200("value").foo();
                """
                    .formatted(packageName))));
  }
}
