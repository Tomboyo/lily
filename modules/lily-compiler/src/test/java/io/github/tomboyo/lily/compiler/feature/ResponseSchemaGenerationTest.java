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
                            type: object
                            properties:
                              foo:
                                type: string
                      content:
                        application/json:
                          schema:
                            type: object
                            properties:
                              foo:
                                type: string
                    '404':
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
                  return (new %s.getfoooperation.GetFoo404(null)) instanceof %s.getfoooperation.GetFooResponse;
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
                  return (new %s.getfoooperation.GetFooDefault(null))
                      instanceof %s.getfoooperation.GetFooResponse;
                  """
                      .formatted(packageName, packageName))));
    }
  }

  /**
   * Response instances like GetFoo200 are wrappers for headers and content, both of which are
   * represented by generated types
   */
  @Nested
  class Content {
    @Test
    void contentForGetFoo200() {
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
    }

    @Test
    void contentForGetFoo404() {
      assertThat(
          "Content is not generated for GetFoo404, since it is already defined",
          true,
          is(
              evaluate(
                  """
              var content = new %s.NotFound("value");
              var response = new %s.getfoooperation.GetFoo404(content);
              return response.content() instanceof %s.NotFound;
              """
                      .formatted(packageName, packageName, packageName))));
    }
  }

  /**
   * Response instances like GetFoo200 are wrappers for headers and content, both of which are
   * represented by generated types
   */
  @Nested
  class Headers {
    @Test
    void headersForGetFoo200() {
      assertThat(
          "The response contains a headers container",
          true,
          is(
              evaluate(
                  """
                      var headers = new %s.getfoooperation.response.GetFoo200Headers(null);
                      var response = new %s.getfoooperation.GetFoo200(null, headers);
                      return response.headers() instanceof %s.getfoooperation.response.GetFoo200Headers;
                      """
                      .formatted(packageName, packageName, packageName))));
    }

    @Test
    void namedHeaders() {
      assertThat(
          "The GetFoo200 headers container exposes named headers",
          true,
          is(
              evaluate(
                  """
                var headers = new %s.getfoooperation.response.GetFoo200Headers(
                  new %s.getfoooperation.response.getfoo200headers.XFooHeader("value"));
                return headers.xFoo().foo() instanceof String;
                """
                      .formatted(packageName, packageName))));
    }
  }
}
