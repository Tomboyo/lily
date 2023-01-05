package io.github.tomboyo.lily.compiler.feature;

import static io.github.tomboyo.lily.compiler.CompilerSupport.compileOas;
import static io.github.tomboyo.lily.compiler.CompilerSupport.deleteGeneratedSources;
import static io.github.tomboyo.lily.compiler.CompilerSupport.evaluate;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Lily generates code from response schema. */
@Nested
class ResponseSchemaGenerationTest {

  /**
   * Response schema combine to form a sealed interface, which helps the IDE make suggestions and
   * supports pattern matching.
   */
  @Nested
  class ResponsesFormASealedInterface {

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
                        description: 'Ok'
                      '404':
                        description: 'Not Found'
                      default:
                        description: 'Default'
              """);
    }

    @Test
    void okResponse() {
      assertTrue(
          evaluate(
              """
                  return %s.getfoooperation.GetFooResponse.class.isAssignableFrom(%s.getfoooperation.GetFoo200.class);
                  """
                  .formatted(packageName, packageName),
              Boolean.class),
          "The 200 response is a member of the response sum type");
    }

    @Test
    void notFoundResponse() {
      assertTrue(
          evaluate(
              """
              return %s.getfoooperation.GetFooResponse.class.isAssignableFrom(%s.getfoooperation.GetFoo404.class);
              """
                  .formatted(packageName, packageName),
              Boolean.class),
          "The 404 response is a member of the response sum type");
    }

    @Test
    void defaultResponse() {
      assertTrue(
          evaluate(
              """
                  return %s.getfoooperation.GetFooResponse.class.isAssignableFrom(%s.getfoooperation.GetFooDefault.class);
                  """
                  .formatted(packageName, packageName),
              Boolean.class),
          "The default response is a member of the response sum type");
    }
  }
}
