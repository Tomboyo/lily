package io.github.tomboyo.lily.compiler.feature;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.tomboyo.lily.compiler.LilyExtension;
import io.github.tomboyo.lily.compiler.LilyExtension.LilyTestSupport;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpResponse;

import io.github.tomboyo.lily.compiler.LilyTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * TODO: update this description<p>
 *
 * Operations expose setters by parameter location, since parameters
 * are only distinct by name and location. Values are bound to parameters using anonymous namespaces
 * to leverge IDE support and avoid making the user import several classes:
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
@WireMockTest
public class ParametersTest {
  @ParameterizedTest
  @CsvSource({
      "/pets/{id},           /pets/12345",
      "/pets/{id}/,          /pets/12345/",
      "/pets/{id}/the/rest,  /pets/12345/the/rest",
      "/pets/{id}/the/rest/, /pets/12345/the/rest/",
      "/{id}/the/rest,       /12345/the/rest",
      "/{id}/the/rest/,      /12345/the/rest/",
      "/{id},                /12345",
      "/{id}/,               /12345/"
  })
  @LilyTest
  void canBindPathParameters(String path, String expected, LilyTestSupport support, WireMockRuntimeInfo wmi) throws IOException {
    support.compileOas(
        """
        paths:
          %s:
            get:
              operationId: getPet
              parameters:
                - name: id
                  in: path
                  style: simple
                  schema:
                    type: string
        """.formatted(path));

    support.evaluate("""
        {{package}}.Api.newBuilder()
          .uri("{{baseUrl}}")
          .build()
          .everyOperation()
          .getPet()
          .withParameters(p -> p.withId("12345"))
          .sendSync();
        return null;
        """, "baseUrl", wmi.getHttpBaseUrl());

    // The user is able to bind path parameters with the operation API.
    verify(getRequestedFor(urlEqualTo(expected)));
  }

  @Test
  @LilyTest
  void pathParametersAreFormatted(LilyTestSupport support, WireMockRuntimeInfo wmi) {
    support.compileOas("""
        paths:
          /pets/{a}{b}{c}{d}:
            get:
              operationId: getPet
              parameters:
                - name: a
                  in: path
                  style: simple
                  schema:
                    type: string
                - name: b
                  in: path
                  style: simple
                  schema:
                    type: string
                - name: c
                  in: path
                  style: form
                  schema:
                    type: string
                - name: d
                  in: path
                  style: form
                  schema:
                    type: string
        """);

    support.evaluate("""
        {{package}}.Api.newBuilder()
          .uri("{{baseUrl}}")
          .build()
          .everyOperation()
          .getPet()
          .withParameters(p -> p
              .withA("a")
              .withB("b")
              .withC("c")
              .withD("d"))
          .sendSync();
        return null;""", "baseUrl", wmi.getHttpBaseUrl());

    /* The path parameters should evaluate like RFC6570 {a,b}{?c,d}, correctly grouping consecutive parameters with the
     * same encoding style. This impacts the placement of separator characters. This is a contrived example. */
    verify(getRequestedFor(urlEqualTo("/pets/a,b?c=c&d=d")));
  }

  @Test
  @LilyTest
  void canBindQueryParameters(LilyTestSupport support, WireMockRuntimeInfo wmi) throws IOException {
    support.compileOas(
        """
        paths:
          /pets:
            get:
              operationId: searchPets
              parameters:
                - name: name
                  in: query
                  style: form
                  schema:
                    type: string
        """);

    support.evaluate("""
        {{package}}.Api.newBuilder()
          .uri("{{baseUrl}}")
          .build()
          .everyOperation()
          .searchPets()
          .withParameters(p -> p.withName("fido"))
          .sendSync();
        return null;
        """, "baseUrl", wmi.getHttpBaseUrl());

    // The user is able to bind query parameters with the operation API.
    verify(getRequestedFor(urlEqualTo("/pets?name=fido")));
  }

  @ExtendWith(LilyExtension.class)
  @Nested
  class CanSetEachTypeOfParameter {
    @BeforeAll
    static void beforeAll(LilyTestSupport support) throws Exception {
      support.compileOas(
          """
          paths:
            /pets/{id}:
              get:
                operationId: getPet
                parameters:
                  - name: id
                    in: path
                    style: simple
                    schema:
                      type: string
                  - name: id
                    in: path
                    style: simple
                    schema:
                      type: string
                  - name: id
                    in: path
                    style: simple
                    schema:
                      type: string
                  - name: id
                    in: path
                    style: simple
                    schema:
                      type: string
          """);
    }

    // TODO: test multiple arity! Especially for query parameters and when some are null.

    @Test
    void canSetPathParameters(LilyTestSupport support) {
      // TODO:
      //   - the queryString/pathString/etc functions should be re-implemented to use the
      // setParameters data
      //   - make sure to check whether each parameter is actually bound before interpolating it,
      // otherwise it produces
      //     NPEs like we have now (sigh)
      var uri =
          support.evaluate(
              """
              return {{package}}.Api.newBuilder()
                .uri("https://www.example.com/")
                .build()
                .everyOperation()
                .getPet()
                .setParameters(p -> p.withId("id"))
                .httpRequest()
                .uri();
              """,
              URI.class);
      assertEquals("https://www.example.com/pets/id", uri.toString(), "Can bind path parameters");
    }
  }

  @Nested
  class WhenSchemaObject {
    @Nested
    @ExtendWith(LilyExtension.class)
    class WhenInPath {
      @BeforeAll
      static void beforeAll(LilyTestSupport support) throws Exception {
        support.compileOas(
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
      void path(LilyTestSupport support) {
        var actual =
            support.evaluate(
                """
                return {{package}}.Api.newBuilder()
                  .uri("https://example.com/")
                  .build()
                  .everyOperation()
                  .getPetFromKennel()
                  .path(path ->
                      path.kennelId("kennelId")
                          .petId("petId"))
                  .httpRequest()
                  .uri();
                """,
                URI.class);
        assertThat(
            "Named path parameters are bound by the path method",
            actual,
            is(URI.create("https://example.com/pets/kennelId/petId")));
      }

      @Test
      void pathString(LilyTestSupport support) {
        var actual =
            support.evaluate(
                """
                return {{package}}.Api.newBuilder()
                  .uri("https://example.com/")
                  .build()
                  .everyOperation()
                  .getPetFromKennel()
                  .path(path ->
                      path.kennelId("kennelId")
                          .petId("petId"))
                  .pathString();
                """,
                String.class);
        assertEquals(
            "pets/kennelId/petId",
            actual,
            "pathString() returns the interpolated path part of the configured operation");
      }
    }

    @Nested
    @ExtendWith(LilyExtension.class)
    class WhenInQuery {
      private static String packageName;

      @BeforeAll
      static void beforeAll(LilyTestSupport support) throws Exception {
        support.compileOas(
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
      void query(LilyTestSupport support) {
        var actual =
            support.evaluate(
                """
                return {{package}}.Api.newBuilder()
                  .uri("https://example.com/")
                  .build()
                  .everyOperation()
                  .listPets()
                  .query(query -> query
                      .limit(5)
                      .include(java.util.List.of("name", "age")))
                  .httpRequest()
                  .uri();
                """,
                URI.class);
        assertThat(
            "Query parameters are bound by the query method",
            actual,
            is(URI.create("https://example.com/pets?include=name&include=age&limit=5")));
      }

      @Test
      void queryString(LilyTestSupport support) {
        var actual =
            support.evaluate(
                """
                return {{package}}.Api.newBuilder()
                  .uri("https://example.com/")
                  .build()
                  .everyOperation()
                  .listPets()
                  .query(query -> query
                      .limit(5)
                      .include(java.util.List.of("name", "age")))
                  .queryString();
                """,
                String.class);

        assertEquals(
            "?include=name&include=age&limit=5",
            actual,
            "queryString() returns the interpolated query string of the configured operation");
      }
    }
  }

  @Nested
  @ExtendWith(LilyExtension.class)
  class WhenRefObject {
    @BeforeAll
    static void beforeAll(LilyTestSupport support) {
      support.compileOas(
          """
          openapi: 3.0.2
          paths:
            /pets:
              get:
                operationId: listPets
                parameters:
                  - $ref: "#/components/schemas/Foo"
                  - name: limit
                    in: query
                    schema:
                      type: integer
                      format: int32
          """);
    }

    @Test
    void refIsSkipped(LilyTestSupport support) {
      assertDoesNotThrow(
          () ->
              support.evaluate(
                  """
                  return {{package}}.Api.newBuilder()
                    .uri("https://example.com/")
                    .build()
                    .everyOperation()
                    .listPets()
                    .query(query -> query
                        .limit(5));
                  """),
          "Unsupported parameter specifications do not prevent evaluation of supported"
              + " parameters.");
    }
  }
}
