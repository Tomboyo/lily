package io.github.tomboyo.lily.compiler.feature;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tomboyo.lily.compiler.LilyExtension;
import io.github.tomboyo.lily.compiler.LilyExtension.LilyTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Lily generates code from response schema. */
@Nested
class ResponseSchemaGenerationTest {

  /**
   * Response schema combine to form a sealed interface, which helps the IDE make suggestions and
   * supports pattern matching.
   */
  @Nested
  @ExtendWith(LilyExtension.class)
  class ResponsesFormASealedInterface {

    private static String packageName;

    @BeforeAll
    static void beforeAll(LilyTestSupport support) throws Exception {
      support.compileOas(
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
    void okResponse(LilyTestSupport support) {
      assertTrue(
          support.evaluate(
              """
              return {{package}}.getfoooperation.GetFooResponse.class.isAssignableFrom(
                  {{package}}.getfoooperation.GetFoo200.class);
              """,
              Boolean.class),
          "The 200 response is a member of the response sum type");
    }

    @Test
    void notFoundResponse(LilyTestSupport support) {
      assertTrue(
          support.evaluate(
              """
              return {{package}}.getfoooperation.GetFooResponse.class.isAssignableFrom(
                  {{package}}.getfoooperation.GetFoo404.class);
              """,
              Boolean.class),
          "The 404 response is a member of the response sum type");
    }

    @Test
    void defaultResponse(LilyTestSupport support) {
      assertTrue(
          support.evaluate(
              """
              return {{package}}.getfoooperation.GetFooResponse.class.isAssignableFrom(
                  {{package}}.getfoooperation.GetFooDefault.class);
              """,
              Boolean.class),
          "The default response is a member of the response sum type");
    }
  }

  @Nested
  @ExtendWith(LilyExtension.class)
  class MalformedResponses {
    @Test
    void missingMediaType(LilyTestSupport support) {
      assertDoesNotThrow(
          () ->
              support.compileOas(
                  """
                  paths:
                    /foo:
                      get:
                        operationId: getFoo
                        responses:
                          '200':
                            content:
                              'application/json':
                                # missing MediaType specification
                  """));
    }
  }
}
