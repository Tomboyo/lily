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
                      headers:
                        x-foo:
                          schema:
                            type: string
                      content:
                        application/json:
                          schema:
                            type: object
                            properties:
                              foo:
                                type: string
                    '404':
                      #headers:
                      #  x-foo:
                      #    $ref: '#/components/schemas/MyHeader'
                      content:
                        application/json:
                          schema:
                            $ref: '#/components/schemas/NotFound'
                    default:
                      content:
                        application/json:
                          schema:
                            $ref: '#/components/schemas/NotFound'
            components:
              schemas:
                NotFound:
                  type: string
                MyHeader:
                  type: string
            """);
  }

  /**
   * Response schema combine to form a sealed interface, which helps the IDE make suggestions and
   * supports pattern matching.
   */
  @Nested
  class ResponsesFormASealedInterface {
    @Test
    void okResponse() {
      assertThat(
          "The 200 response is a member of the response sum type",
          true,
          is(
              evaluate(
                  """
                  return (new %s.getfoooperation.GetFoo200(null, null)) instanceof %s.getfoooperation.GetFooResponse;
                  """
                      .formatted(packageName, packageName))));
    }

    @Test
    void notFoundResponse() {
      assertThat(
          "The 404 response is a member of the response sum type",
          true,
          is(
              evaluate(
                  """
                  return (new %s.getfoooperation.GetFoo404(null, null)) instanceof %s.getfoooperation.GetFooResponse;
                  """
                      .formatted(packageName, packageName))));
    }

    @Test
    void defaultResponse() {
      assertThat(
          "The default response is a member of the response sum type",
          true,
          is(
              evaluate(
                  """
                  return (new %s.getfoooperation.GetFooDefault(null, null))
                      instanceof %s.getfoooperation.GetFooResponse;
                  """
                      .formatted(packageName, packageName))));
    }
  }

  /**
   * Response instances like GetFoo200 are wrappers for headers and content, both of which are
   * represented by generated types
   */
  @Test
  void content() {
    assertThat(
        "Content is generated for GetFoo200",
        true,
        is(
            evaluate(
                """
            var content = new %s.getfoooperation.response.GetFoo200Content("value");
            var response = new %s.getfoooperation.GetFoo200(content, null);
            return response.content() instanceof %s.getfoooperation.response.GetFoo200Content;
            """
                    .formatted(packageName, packageName, packageName))));

    assertThat(
        "But content is not generated for GetFoo404, since it is already defined",
        true,
        is(
            evaluate(
                """
            var content = new %s.NotFound("value");
            var response = new %s.getfoooperation.GetFoo404(content, null);
            return response.content() instanceof %s.NotFound;
            """
                    .formatted(packageName, packageName, packageName))));
  }

  /**
   * Response instances like GetFoo200 are wrappers for headers and content, both of which are
   * represented by generated types
   */
  @Test
  void headers() {
    assertThat(
        "The response contains headers",
        true,
        is(
            evaluate(
                """
            var headers = new %s.getfoooperation.response.GetFoo200Headers();
            var response = new %s.getfoooperation.GetFoo200(null, headers);
            return response.headers() instanceof %s.getfoooperation.response.GetFoo200Headers;
            """
                    .formatted(packageName, packageName, packageName))));

    //    assertThat(
    //        true,
    //        is(evaluate(
    //            """
    //            var headers = new %s.getfoooperation.GetFoo404Headers(
    //              new %s.MyHeader("value"));
    //            var response = new %s.getfoooperation.GetFoo404(null, headers);
    //            return response.headers() instanceof %s.GetFoo404Headers;
    //            """.formatted(packageName, packageName, packageName, packageName)
    //        )));
  }
}
