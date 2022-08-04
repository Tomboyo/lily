package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.icg.CompilerSupport.compileOas;
import static io.github.tomboyo.lily.compiler.icg.CompilerSupport.deleteGeneratedSources;
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
          info:
            title: MultipleTags
            description: "An operation with multiple tags"
            version: 0.1.0
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
}
