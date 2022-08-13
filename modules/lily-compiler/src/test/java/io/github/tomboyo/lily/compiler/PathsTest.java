package io.github.tomboyo.lily.compiler;

import static io.github.tomboyo.lily.compiler.CompilerSupport.compileOas;
import static io.github.tomboyo.lily.compiler.CompilerSupport.deleteGeneratedSources;
import static io.github.tomboyo.lily.compiler.CompilerSupport.evaluate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests the APIs generated from OAS paths. */
public class PathsTest {

  @AfterAll
  static void afterAll() throws Exception {
    deleteGeneratedSources();
  }

  @Nested
  class TaggedOperationsApiTests {

    private static String packageName;

    @BeforeAll
    static void beforeAll() throws Exception {
      packageName =
          compileOas(
              """
          openapi: 3.0.2
          paths:
            /pets/:
              get:
                operationId: getPets
                tags:
                  - dogs
                  - cats
                responses:
                  "204":
                    description: OK
          """);
    }

    @Test
    void hasOperationsApiForDogsTag() throws Exception {
      assertEquals(
          packageName + ".DogsOperations",
          Class.forName(packageName + ".Api").getMethod("dogsOperations").getReturnType().getName(),
          "api.dogsOperations() returns DogsOperations");
    }

    @Test
    void hasOperationsApiForCatsTag() throws Exception {
      assertEquals(
          packageName + ".CatsOperations",
          Class.forName(packageName + ".Api").getMethod("catsOperations").getReturnType().getName(),
          "api.catsOperations() returns CatsOperations");
    }

    @Test
    void dogsOperationsContainsGetPetsOperation() throws Exception {
      assertEquals(
          packageName + ".GetPetsOperation",
          Class.forName(packageName + ".DogsOperations")
              .getMethod("getPets")
              .getReturnType()
              .getName(),
          "api.dogsOperations().getPets() returns the GetPetsOperation");
    }

    @Test
    void catsOperationsContainsGetPetsOperation() throws Exception {
      assertEquals(
          packageName + ".GetPetsOperation",
          Class.forName(packageName + ".CatsOperations")
              .getMethod("getPets")
              .getReturnType()
              .getName(),
          "api.catsOperations().getPets() returns the GetPetsOperations");
    }
  }

  @Nested
  class ParameterSchemaGeneration {
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
    void pathItemParametersAreGeneratedToTypes() throws Exception {
      assertThat(
          "Lily generates new types for path (item) parameter object schemas",
          "value",
          is(
              evaluate(
                  """
              return new %s.getbyid.Foo("value").foo();
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
              return new %s.getbyid.Bar(true).bar();
              """
                      .formatted(packageName))));
    }
  }
}
