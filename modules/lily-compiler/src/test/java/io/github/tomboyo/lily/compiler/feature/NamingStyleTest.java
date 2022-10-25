package io.github.tomboyo.lily.compiler.feature;

import static io.github.tomboyo.lily.compiler.CompilerSupport.compileOas;
import static io.github.tomboyo.lily.compiler.CompilerSupport.deleteGeneratedSources;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Lily supports identifiers in snake_case, kebab-case, and camelCase (and PascalCase, or "upper
 * camel case" in the code).
 */
public class NamingStyleTest {

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
                  operationId: get-pet
                  parameters:
                    - name: some-parameter
                      in: query
                      schema:
                        type: object
                post:
                  operationId: post_pet
                  parameters:
                    - name: some_parameter
                      in: query
                      schema:
                        type: object
                delete:
                  operationId: deletePet
                  parameters:
                    - name: SomeParameter
                      in: query
                      schema:
                        type: object
            """);
  }

  @ParameterizedTest
  @ValueSource(strings = {"GetPetOperation", "PostPetOperation", "DeletePetOperation"})
  void operationsDefined(String className) throws Exception {
    assertNotNull(Class.forName(packageName + "." + className));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "getpetoperation.SomeParameter",
        "postpetoperation.SomeParameter",
        "deletepetoperation.SomeParameter"
      })
  void parametersDefined(String namePart) throws Exception {
    assertNotNull(Class.forName(packageName + "." + namePart));
  }
}
