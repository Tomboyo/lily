package io.github.tomboyo.lily.compiler.feature;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.github.tomboyo.lily.compiler.LilyExtension;
import io.github.tomboyo.lily.compiler.LilyExtension.LilyTestSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

@ExtendWith(LilyExtension.class)
@ExtendWith(WireMockExtension.class)
public class RequestBodyTest {

  @Nested
  class RequestBodyComponentSchema {
    @BeforeAll
    static void beforeAll(LilyTestSupport support) {
      support.compileOas(
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
                        $ref: '#/components/schemas/Pet'
          components:
            schemas:
              Pet:
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
    void sendSync(LilyTestSupport support, WireMockRuntimeInfo info) {
      support.evaluate(
          """
          return {{package}}.Api.newBuilder()
            .uri("{{uri}}")
            .build()
            .everyOperation()
            .createPet()
            .body(new {{package}}.Pet("Fido", 12))
            .sendSync();
          """,
          "uri", info.getHttpBaseUrl());

      /* When a $ref requestBody schema is specified, Lily generates a corresponding "anonymous"
       * body in the base package and an operation with a #body(...) method. When sendSync is
       * invoked, the operation will set the content-type header to application/json and attach the
       * json-encoded body. (Because we cannot inspect the body of an HttpRequest directly, we
       * dispatch it with HttpClient and verify with Wiremock instead.) */
      verify(
          putRequestedFor(urlEqualTo("/pets"))
              .withHeader("content-type", equalTo("application/json"))
              .withRequestBody(equalToJson("{\"name\":\"Fido\",\"age\":12}")));
    }

    @Test
    void httpRequest(LilyTestSupport support, WireMockRuntimeInfo info) throws Exception {
      var request = support.evaluate(
          """
          return {{package}}.Api.newBuilder()
            .uri("{{uri}}")
            .build()
            .everyOperation()
            .createPet()
            .body(new {{package}}.Pet("Fido", 12))
            .httpRequest();
          """,
          HttpRequest.class,
          "uri", info.getHttpBaseUrl());

      HttpClient.newHttpClient().send(request, BodyHandlers.discarding());

      /* When a $ref requestBody schema is specified, Lily generates a corresponding "anonymous"
       * body in the base package and an operation with a #body(...) method. When sendSync is
       * invoked, the operation will set the content-type header to application/json and attach the
       * json-encoded body. */
      verify(
          putRequestedFor(urlEqualTo("/pets"))
              .withHeader("content-type", equalTo("application/json"))
              .withRequestBody(equalToJson("{\"name\":\"Fido\",\"age\":12}")));
    }
  }

  @Nested
  class InLineRequestBodySchema {
    @BeforeAll
    static void beforeAll(LilyTestSupport support) {
      support.compileOas(
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
    void sendSync(LilyTestSupport support, WireMockRuntimeInfo info) {
      support.evaluate(
          """
        return {{package}}.Api.newBuilder()
          .uri("{{url}}")
          .build()
          .everyOperation()
          .createPet()
          .body(new {{package}}.createpetoperation.CreatePetBody("Fido", 12))
          .sendSync();
        """,
          "url", info.getHttpBaseUrl());

      /* When an in-line requestBody schema is specified, Lily generates a corresponding "anonymous"
       * body (named after the operation) and an operation with a #body(...) method. When sendSync
       * is invoked, the operation will set the content-type header to application/json and attach
       * the json-encoded body. */
      verify(
          putRequestedFor(urlEqualTo("/pets"))
              .withHeader("content-type", equalTo("application/json"))
              .withRequestBody(equalToJson("{\"name\":\"Fido\",\"age\":12}")));
    }

    @Test
    void httpRequest(LilyTestSupport support, WireMockRuntimeInfo info) throws Exception {
      var request = support.evaluate(
          """
          return {{package}}.Api.newBuilder()
            .uri("{{url}}")
            .build()
            .everyOperation()
            .createPet()
            .body(new {{package}}.createpetoperation.CreatePetBody("Fido", 12))
            .httpRequest();
          """,
          HttpRequest.class,
          "url", info.getHttpBaseUrl());

      HttpClient.newHttpClient().send(request, BodyHandlers.discarding());

      /* When an in-line requestBody schema is specified, Lily generates a corresponding "anonymous"
       * body (named after the operation) and an operation with a #body(...) method. When
       * httpRequest is invoked, the operation will return a request with the content-type header
       * set to application/json and with the json-encoded body attached. (Because we cannot inspect
       * the body of an HttpRequest directly, we dispatch it with HttpClient and verify with
       * Wiremock instead.) */
      verify(
          putRequestedFor(urlEqualTo("/pets"))
              .withHeader("content-type", equalTo("application/json"))
              .withRequestBody(equalToJson("{\"name\":\"Fido\",\"age\":12}")));
    }
  }
}
