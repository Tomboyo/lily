package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.icg.CompilerSupport.generateAndCompile;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Tests the APIs generated from OAS paths. */
public class PathsTest {

  @BeforeAll
  public static void beforeAll() throws Exception {
    generateAndCompile(
        "com.example.pathstest",
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
  public void foo() throws Exception {
    assertEquals(
        "com.example.pathstest.DogsOperations",
        Class.forName("com.example.pathstest.Api")
            .getMethod("dogsOperations")
            .getReturnType()
            .getName(),
        "api.dogsOperations() returns DogsOperations");

    assertEquals(
        "com.example.pathstest.CatsOperations",
        Class.forName("com.example.pathstest.Api")
            .getMethod("catsOperations")
            .getReturnType()
            .getName(),
        "api.catsOperations() returns CatsOperations");

    assertEquals(
        "com.example.pathstest.GetPetsOperation",
        Class.forName("com.example.pathstest.DogsOperations")
            .getMethod("getPets")
            .getReturnType()
            .getName(),
        "api.dogsOperations().getPets() returns the GetPetsOperation");

    assertEquals(
        "com.example.pathstest.GetPetsOperation",
        Class.forName("com.example.pathstest.CatsOperations")
            .getMethod("getPets")
            .getReturnType()
            .getName(),
        "api.catsOperations().getPets() returns the GetPetsOperations");
  }
}
