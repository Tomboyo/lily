package io.github.tomboyo.lily.compiler.feature;

import static io.github.tomboyo.lily.compiler.CompilerSupport.compileOas;
import static io.github.tomboyo.lily.compiler.CompilerSupport.deleteGeneratedSources;
import static io.github.tomboyo.lily.compiler.CompilerSupport.evaluate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Lily generates classes from response schema. */
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
                  return (new %s.getfoooperation.GetFoo200()) instanceof %s.getfoooperation.GetFooResponse;
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
                      return (new %s.getfoooperation.GetFoo404()) instanceof %s.getfoooperation.GetFooResponse;
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
                  return (new %s.getfoooperation.GetFooDefault()) instanceof %s.getfoooperation.GetFooResponse;
                  """
                  .formatted(packageName, packageName),
              Boolean.class),
          "The default response is a member of the response sum type");
    }
  }

  @Nested
  class ResponseContent {

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
                          'application/json':
                            schema:
                              type: object
                              properties:
                                value:
                                  type: string
                      '404':
                        content:
                          'application/json':
                            schema:
                              $ref: '#/components/schemas/MyContent'
              components:
                schemas:
                  MyContent:
                    type: string
              """);
    }

    @Test
    void inLineSchemaContent() {
      assertEquals(
          "value",
          evaluate(
              """
                  var content = new %s.getfoooperation.response.GetFoo200Content("value");
                  var response = new %s.getfoooperation.GetFoo200(content);
                  return response.content().value();
                  """
                  .formatted(packageName, packageName)),
          "Generated content is part of the Response API");
    }

    @Test
    void refContent() {
      assertEquals(
          "value",
          evaluate(
              """
                  var content = new %s.MyContent("value");
                  var response = new %s.getfoooperation.GetFoo404(content);
                  return response.content().value();
                  """
                  .formatted(packageName, packageName)),
          "Referenced content is part of the Response API");
    }
  }

  /** Response instances are wrappers for declared headers. */
  @Nested
  class Headers {

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
              components:
                schemas:
                  MyHeader:
                    type: string
              """);
    }

    @Test
    void inLineSchemaHeaders() {
      assertEquals(
          "value",
          evaluate(
              """
                      var headers = new %s.getfoooperation.response.GetFoo200Headers("value");
                      var response = new %s.getfoooperation.GetFoo200(headers);
                      return response.headers().xFoo();
                      """
                  .formatted(packageName, packageName)),
          "Generated headers are part of the Response API");
    }
  }
}
