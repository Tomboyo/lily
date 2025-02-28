package io.github.tomboyo.lily.compiler.feature;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static io.github.tomboyo.lily.compiler.CompilerSupport.compileOas;
import static io.github.tomboyo.lily.compiler.CompilerSupport.evaluate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@WireMockTest
class SynchronousRequestTests {

  @AfterAll
  static void afterAll() throws Exception {
    //    deleteGeneratedSources();
  }

  @Nested
  class WhenResponseHasContent {
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
                    tags:
                      - pet
                    responses:
                      '200':
                        headers:
                          x-my-header:
                            schema:
                              type: string
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
    void sendSyncExposesHttpResponse(WireMockRuntimeInfo info) {
      stubFor(
          get("/pets")
              .willReturn(
                  ok("""
                  { "name": "Fido", "age": 5 }
                  """)
                      .withHeader("x-my-header", "value")));

      var actual =
          evaluate(
              """
              return %s.Api.newBuilder()
                  .uri("%s")
                  .build()
                  .petOperations()
                  .listPets()
                  .sendSync()
                  .httpResponse();
              """
                  .formatted(packageName, info.getHttpBaseUrl()),
              HttpResponse.class);

      // When responses are defined, the native http response is exposed by the
      // generated response type
      assertEquals(200, actual.statusCode());
      assertEquals("value", actual.headers().firstValue("x-my-header").orElseThrow());
      assertTrue(actual.body() instanceof InputStream);
    }

    @Test
    void sendSyncDeserializesBody(WireMockRuntimeInfo info) throws Exception {
      stubFor(
          get("/pets")
              .willReturn(
                  ok("""
                  { "name": "Fido", "age": 5 }
                  """)
                      .withHeader("x-my-header", "value")));

      var actual =
          evaluate(
              """
              return %s.Api.newBuilder()
                  .uri("%s")
                  .build()
                  .petOperations()
                  .listPets()
                  .sendSync();
              """
                  .formatted(packageName, info.getHttpBaseUrl()));

      assertTrue(
          Class.forName(packageName + ".listpetsoperation.ListPets200")
              .isAssignableFrom(actual.getClass()),
          "The send sync response contains the deserialized response body");

      var actualBody = actual.getClass().getMethod("body").invoke(actual);
      assertEquals(
          "Fido",
          actualBody.getClass().getMethod("name").invoke(actualBody),
          "The name field is deserialized");

      assertEquals(
          5,
          actualBody.getClass().getMethod("age").invoke(actualBody),
          "The age field is deserialized");
    }

    @Test
    void sendSyncDeserializationIsLazy(WireMockRuntimeInfo info) {
      stubFor(
          get("/pets")
              .willReturn(
                  ok("""
                  ({[some gibberish that can't be deserialized
                  """)
                      .withHeader("x-my-header", "value")));

      var actual =
          evaluate(
              """
              return %s.Api.newBuilder()
                  .uri("%s")
                  .build()
                  .petOperations()
                  .listPets()
                  .sendSync();
              """
                  .formatted(packageName, info.getHttpBaseUrl()));

      var e =
          assertThrows(
              InvocationTargetException.class,
              () -> actual.getClass().getMethod("body").invoke(actual),
              """
              If the response content cannot be deserialized, the request should still succeed. Only
              when .body() is invoked should an exception be raised so that the user can still use
              the response object and its headers, and even use the native httpResponse to inspect
              the content.
              """);
      assertTrue(e.getCause() instanceof IOException);
    }
  }

  @Nested
  class WhenResponseHasNoContent {
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
                    tags:
                      - pet
                    responses:
                      '204':
                        description: 'no content'
              """);
    }

    @Test
    void sendSyncExposesHttpResponse(WireMockRuntimeInfo info) {
      stubFor(get("/pets").willReturn(noContent().withHeader("x-my-header", "value")));

      var actual =
          evaluate(
              """
              return %s.Api.newBuilder()
                  .uri("%s")
                  .build()
                  .petOperations()
                  .listPets()
                  .sendSync()
                  .httpResponse();
              """
                  .formatted(packageName, info.getHttpBaseUrl()),
              HttpResponse.class);

      // When responses are defined, the native http response is exposed by the
      // generated response type
      assertEquals(204, actual.statusCode());
      assertEquals("value", actual.headers().firstValue("x-my-header").orElseThrow());
    }

    @Test
    void sendSyncHasNoBodyGetter() {
      assertThrows(
          NoSuchMethodException.class,
          () -> Class.forName(packageName + ".listpetsoperation.ListPets204").getMethod("body"),
          "ListPets204 should not have a body() method because the API returns no content");
    }
  }

  @Nested
  class WhenUnexpectedResponse {
    @Test
    void usesProvidedDefault(WireMockRuntimeInfo info) throws Exception {
      var packageName =
          compileOas(
              """
              openapi: 3.0.2
              paths:
                /pets:
                  get:
                    operationId: listPets
                    tags:
                      - pet
                    responses:
                      'default':
                        description: 'default response'
              """);

      var actual =
          evaluate(
              """
              return %s.Api.newBuilder()
                  .uri("%s")
                  .build()
                  .petOperations()
                  .listPets()
                  .sendSync();
              """
                  .formatted(packageName, info.getHttpBaseUrl()));

      assertTrue(
          Class.forName(packageName + ".listpetsoperation.ListPetsDefault")
              .isAssignableFrom(actual.getClass()),
          "If the OAS defines a default, it is used to hold unexpected responses");
    }

    @Test
    void usesDefault(WireMockRuntimeInfo info) throws Exception {
      var packageName =
          compileOas(
              """
              openapi: 3.0.2
              paths:
                /pets:
                  get:
                    operationId: listPets
                    tags:
                      - pet
                    responses:
              """);

      var actual =
          evaluate(
              """
              return %s.Api.newBuilder()
                  .uri("%s")
                  .build()
                  .petOperations()
                  .listPets()
                  .sendSync();
              """
                  .formatted(packageName, info.getHttpBaseUrl()));

      assertTrue(
          Class.forName(packageName + ".listpetsoperation.ListPetsDefault").isInstance(actual),
          "If the OAS does not define a default, one is generated anyway and used to hold"
              + " unexpected responses");
    }
  }

  /*
   * After a user customizes an HTTP request, they can still use the operation's sendSync method to
   * dispatch the request and deserialize the response into a Response object.
   */
  @Test
  void customizedRequest(WireMockRuntimeInfo info) throws Exception {
    var packageName =
        compileOas(
            """
            openapi: 3.0.2
            paths:
              /pets:
                get:
                  operationId: listPets
                  responses:
                    '200':
                      content:
                        'application/json':
                          schema:
                            type: string
            """);

    stubFor(get("/pets").willReturn(ok("\"expected\"")));

    var actual =
        evaluate(
            """
            var operation = %s.Api.newBuilder()
              .uri("%s")
              .build()
              .everyOperation()
              .listPets();
            var response = operation.sendSync(
                operation.httpRequest());
            if (response instanceof %s.listpetsoperation.ListPets200 ok) {
              return ok.body();
            }
            throw new RuntimeException("Expected 200 OK response");
            """
                .formatted(packageName, info.getHttpBaseUrl(), packageName),
            String.class);

    assertEquals(
        "expected",
        actual,
        "The user may customize an http request and send it with the operation's sendSync method");
  }
}
