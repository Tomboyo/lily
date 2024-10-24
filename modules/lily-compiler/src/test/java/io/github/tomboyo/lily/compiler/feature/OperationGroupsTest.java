package io.github.tomboyo.lily.compiler.feature;

import static io.github.tomboyo.lily.compiler.CompilerSupport.compileOas;
import static io.github.tomboyo.lily.compiler.CompilerSupport.deleteGeneratedSources;
import static io.github.tomboyo.lily.compiler.CompilerSupport.evaluate;
import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** The API groups operations together based on their OpenAPI tags. */
public class OperationGroupsTest {

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
              /pets/:
                get:
                  operationId: getPet
                  tags:
                    - dog
                    - cat
                post:
                  operationId: postPet
                  tags:
                    - dog
                put:
                  operationId: putPet
            """);
  }

  @Test
  void operationsGrupedByTags() throws Exception {
    var getPetOperation = Class.forName(packageName + ".GetPetOperation");
    var postPetOperation = Class.forName(packageName + ".PostPetOperation");

    assertEquals(
        getPetOperation,
        evaluate(
                """
                return %s.Api.newBuilder()
                  .uri("https://example.com/")
                  .build()
                  .dogOperations()
                  .getPet();
                """
                    .formatted(packageName))
            .getClass(),
        "The getPet operation is grouped under dogOperations() because it has the dog tag");
    assertEquals(
        getPetOperation,
        evaluate(
                """
                return %s.Api.newBuilder()
                  .uri("https://example.com/")
                  .build()
                  .catOperations()
                  .getPet();
                """
                    .formatted(packageName))
            .getClass(),
        "The getPet operation is grouped under catOperations() because it also has the cat tag");
    assertEquals(
        postPetOperation,
        evaluate(
                """
                return %s.Api.newBuilder()
                    .uri("https://example.com/")
                    .build()
                    .dogOperations()
                    .postPet();
                """
                    .formatted(packageName))
            .getClass(),
        "The postPet operation is grouped under dogOperations() because it has the dog tag");
    assertTrue(
        Arrays.stream(
                evaluate(
                        """
                        return %s.Api.newBuilder()
                            .uri("https://example.com/")
                            .build()
                            .catOperations();
                        """
                            .formatted(packageName))
                    .getClass()
                    .getDeclaredMethods())
            .noneMatch(method -> method.getName().equals("postPet")),
        "The postPet operation is not grouped under catOperations() because it does not have the"
            + " cat tag");
  }

  @Test
  void everyOperation() {
    var methods =
        evaluate(
                """
                return %s.Api.newBuilder()
                    .uri("https://example.com/")
                    .build()
                    .everyOperation()
                    .getClass();
                """
                    .formatted(packageName),
                Class.class)
            .getDeclaredMethods();

    assertEquals(
        Set.of("getPet", "postPet", "putPet"),
        Arrays.stream(methods)
            .filter(it -> !it.isSynthetic())
            .map(Method::getName)
            .collect(toSet()),
        "All operations are part of the everyOperation() group");
  }

  @Test
  void everyUntaggedOperation() {
    var methods =
        evaluate(
                """
                return %s.Api.newBuilder()
                    .uri("https://example.com/")
                    .build()
                    .everyUntaggedOperation()
                    .getClass();
                """
                    .formatted(packageName),
                Class.class)
            .getDeclaredMethods();

    assertTrue(
        Arrays.stream(methods).anyMatch(m -> m.getName().equals("putPet")),
        "The putPet operation is grouped under everyUntaggedOperation() because it has no tags");
    assertTrue(
        Arrays.stream(methods).noneMatch(m -> m.getName().equals("getPet")),
        "Only operations without tags are grouped under everyUntaggedOperation()");
  }
}
