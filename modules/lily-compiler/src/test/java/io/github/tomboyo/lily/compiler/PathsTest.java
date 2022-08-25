package io.github.tomboyo.lily.compiler;

import static io.github.tomboyo.lily.compiler.CompilerSupport.compileOas;
import static io.github.tomboyo.lily.compiler.CompilerSupport.deleteGeneratedSources;
import static io.github.tomboyo.lily.compiler.CompilerSupport.evaluate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;

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
                operationId: getPet
                tags:
                  - dogs
                  - cats
              post:
                operationId: postPet
          """);
    }

    @Test
    void hasOperationsApiForDogsTag() throws Exception {
      assertThat(
          "The getPet operation is addressable via the dogs tag, DogsOperations",
          evaluate(
              """
            return new %s.Api().dogsOperations().getPet();
          """
                  .formatted(packageName)),
          isA(Class.forName(packageName + ".GetPetOperation")));
    }

    @Test
    void hasOperationsApiForCatsTag() throws Exception {
      assertThat(
          "The getPet operation is addressable via the cats tag, CatsOperations",
          evaluate(
              """
            return new %s.Api().catsOperations().getPet();
          """
                  .formatted(packageName)),
          isA(Class.forName(packageName + ".GetPetOperation")));
    }

    @Test
    void hasOperationsApiForUntaggedOperations() throws Exception {
      assertThat(
          "The postPet operation is addressable via the other tag, OtherOperations, by default",
          evaluate(
              """
              return new %s.Api().otherOperations().postPet();
              """
                  .formatted(packageName)),
          isA(Class.forName(packageName + ".PostPetOperation")));
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
    void pathItemParametersAreGeneratedToTypes() {
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