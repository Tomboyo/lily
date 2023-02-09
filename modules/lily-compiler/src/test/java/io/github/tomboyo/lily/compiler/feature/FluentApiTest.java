package io.github.tomboyo.lily.compiler.feature;

import static io.github.tomboyo.lily.compiler.CompilerSupport.compileOas;
import static io.github.tomboyo.lily.compiler.CompilerSupport.deleteGeneratedSources;
import static io.github.tomboyo.lily.compiler.CompilerSupport.evaluate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** The fluent API supports chaining calls to prepare and execute requests. */
public class FluentApiTest {

  @AfterAll
  static void afterAll() throws Exception {
    deleteGeneratedSources();
  }

  /**
   * To facilitate temporary work-arounds, users can access the underlying UriTemplate from
   * operations. This allows users to override and otherwise customize request paths and queries.
   */
  @Nested
  class ExposesUnderlyingUriTemplates {
    private static String packageName;

    @BeforeAll
    static void beforeAll() throws Exception {
      packageName =
          compileOas(
              """
              openapi: 3.0.2
              paths:
                /pets/{id}:
                  get:
                    operationId: getPetById
                    parameters:
                      - name: id
                        in: path
                        required: true
                        schema:
                          type: string
              """);
    }

    @Test
    void templatesAreExposedForAllOperations() {
      assertThat(
          "Operations' URI templates may be used to create complete paths to a resource",
          evaluate(
              """
              return %s.Api.newBuilder()
                .uri("https://example.com/")
                .build()
                .everyOperation()
                .getPetById()
                .uriTemplate()
                .bind("id", "some-uuid-here")
                .toURI();
              """
                  .formatted(packageName)),
          is(URI.create("https://example.com/pets/some-uuid-here")));
    }
  }

  /** Lily generates fluent-style setters for all path parameters. */
  @Nested
  class HasFluentPathParameters {
    private static String packageName;

    @BeforeAll
    static void beforeAll() throws Exception {
      packageName =
          compileOas(
              """
              openapi: 3.0.2
              paths:
                /pets/{id}:
                  get:
                    operationId: getPetById
                    parameters:
                      - name: id
                        in: path
                        required: true
                        schema:
                          type: string
              """);
    }

    @Test
    void hasPathParameterSetters() {
      var actual =
          evaluate(
              """
              return %s.Api.newBuilder()
                .uri("https://example.com/")
                .build()
                .everyOperation()
                .getPetById()
                .setId("1234")
                .uriTemplate()
                .toURI();
              """
                  .formatted(packageName),
              URI.class);
      assertThat(
          "Named path parameters may be set via the operation API",
          actual,
          is(URI.create("https://example.com/pets/1234")));
    }
  }

  /** Lily generates fluent-style setters for all query parameters. */
  @Nested
  class HasFluentQueryParameters {
    private static String packageName;

    @BeforeAll
    static void beforeAll() throws Exception {
      packageName =
          compileOas(
              """
              openapi: 3.0.2
              paths:
                /pets:
                  get:
                    operationId: listPets
                    parameters:
                      - name: limit
                        in: query
                        schema:
                          type: integer
                          format: int32
                        # Path parameters are simple by default
                        # style: simple
                      - name: include
                        in: query
                        schema:
                          type: array
                          items:
                            type: string
                        # Query parameters are form-explode by default
                        # style: form
                        # explode: true
              """);
    }

    @Test
    void hasQueryParameterSetters() {
      var actual =
          evaluate(
              """
              return %s.Api.newBuilder()
                .uri("https://example.com/")
                .build()
                .everyOperation()
                .listPets()
                .setLimit(5)
                .setInclude(java.util.List.of("name", "age"))
                .uriTemplate()
                .toURI();
              """
                  .formatted(packageName),
              URI.class);
      assertThat(
          "Query parameters may be set via the operation API",
          actual,
          is(URI.create("https://example.com/pets?include=name&include=age&limit=5")));
    }
  }
}
