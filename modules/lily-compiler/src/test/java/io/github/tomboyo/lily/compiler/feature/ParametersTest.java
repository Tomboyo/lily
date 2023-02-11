package io.github.tomboyo.lily.compiler.feature;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.github.tomboyo.lily.compiler.CompilerSupport.compileOas;
import static io.github.tomboyo.lily.compiler.CompilerSupport.deleteGeneratedSources;
import static io.github.tomboyo.lily.compiler.CompilerSupport.evaluate;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Operations expose setters by parameter location, since parameters are only distinct by name and
 * location. Values are bound to parameters using anonymous namespaces to leverge IDE support and
 * avoid making the user import several classes:
 *
 * <pre>{@code
 * myOperation
 *   .path(path ->
 *       path.foo("1234")
 *           .bar("5678"))
 *   .query(query ->
 *       query.foo("abcd"))
 * }</pre>
 *
 * <p>Operations also expose {@code pathString()} and {@code queryString()} getters so that the user
 * can reuse these interpolated request components while customizing http requests with the native
 * API, such as when working around OpenAPI specification flaws:
 *
 * <pre>{@code
 * var operation = myOperation
 *   .path(path -> path.id("id"))
 *   .query(query -> query.include(List.of("a", "b")));
 * var request = HttpRequest.newBuilder(
 *     operation.httpRequest(),
 *     (k, v) -> true)
 *   .uri("https://example/com/foo/" + operation.pathString() + operation.queryString())
 *   .build();
 * }</pre>
 */
public class ParametersTest {

  @AfterAll
  static void afterAll() throws Exception {
    deleteGeneratedSources();
  }

  @Nested
  class PathParameters {
    private static String packageName;

    @BeforeAll
    static void beforeAll() throws Exception {
      packageName =
          compileOas(
              """
              openapi: 3.0.2
              paths:
                /pets/{kennelId}/{petId}:
                  get:
                    operationId: getPetFromKennel
                    parameters:
                      - name: kennelId
                        in: path
                        required: true
                        schema:
                          type: string
                      - name: petId
                        in: path
                        required: true
                        schema:
                          type: string
              """);
    }

    @Test
    void path() {
      var actual =
          evaluate(
              """
              return %s.Api.newBuilder()
                .uri("https://example.com/")
                .build()
                .everyOperation()
                .getPetFromKennel()
                .path(path ->
                    path.kennelId("kennelId")
                        .petId("petId"))
                .httpRequest()
                .uri();
              """
                  .formatted(packageName),
              URI.class);
      assertThat(
          "Named path parameters are bound by the path method",
          actual,
          is(URI.create("https://example.com/pets/kennelId/petId")));
    }

    @Test
    void pathString() {
      var actual =
          evaluate(
              """
              return %s.Api.newBuilder()
                .uri("https://example.com/")
                .build()
                .everyOperation()
                .getPetFromKennel()
                .path(path ->
                    path.kennelId("kennelId")
                        .petId("petId"))
                .pathString();
              """
                  .formatted(packageName),
              String.class);
      assertEquals(
          "pets/kennelId/petId",
          actual,
          "pathString() returns the interpolated path part of the configured operation");
    }
  }

  @Nested
  class QueryParameters {
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
    void query() {
      var actual =
          evaluate(
              """
              return %s.Api.newBuilder()
                .uri("https://example.com/")
                .build()
                .everyOperation()
                .listPets()
                .query(query -> query
                    .limit(5)
                    .include(java.util.List.of("name", "age")))
                .httpRequest()
                .uri();
              """
                  .formatted(packageName),
              URI.class);
      assertThat(
          "Query parameters are bound by the query method",
          actual,
          is(URI.create("https://example.com/pets?include=name&include=age&limit=5")));
    }

    @Test
    void queryString() {
      var actual =
          evaluate(
              """
          return %s.Api.newBuilder()
            .uri("https://example.com/")
            .build()
            .everyOperation()
            .listPets()
            .query(query -> query
                .limit(5)
                .include(java.util.List.of("name", "age")))
            .queryString();
          """
                  .formatted(packageName),
              String.class);

      assertEquals(
          "?include=name&include=age&limit=5",
          actual,
          "queryString() returns the interpolated query string of the configured operation");
    }
  }

  @Nested
  @ExtendWith(WireMockExtension.class)
  class BodyParameters {
    private static String packageName;

    @BeforeAll
    static void beforeAll() throws Exception {
      packageName =
          compileOas(
              """
              openapi: 3.0.2
              paths:
                /pets:
                  put:
                    operationId: createPet
                    requestBody:
                      content:
                        'application/json':
                          schema:
                            type: object
                            properties:
                              name:
                                type: string
                              age:
                                type: integer
                                format: int32
              """);
    }

    @Test
    void body(WireMockRuntimeInfo info) {
      evaluate(
          """
          return %s.Api.newBuilder()
            .uri("%s")
            .build()
            .everyOperation()
            .createPet()
            .body(new %s.createpetoperation.CreatePetBody("Fido", 12))
            .sendSync();
          """
              .formatted(packageName, info.getHttpBaseUrl(), packageName));

      verify(putRequestedFor(urlEqualTo("/pets"))
          .withHeader("content-type", equalTo("application/json"))
          .withRequestBody(equalToJson("{\"name\":\"Fido\",\"age\":12}")));
    }
  }
}
