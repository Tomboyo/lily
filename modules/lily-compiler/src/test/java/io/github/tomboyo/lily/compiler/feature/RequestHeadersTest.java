package io.github.tomboyo.lily.compiler.feature;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import io.github.tomboyo.lily.compiler.LilyExtension;
import io.github.tomboyo.lily.compiler.LilyExtension.LilyTestSupport;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WireMockExtension.class)
public class RequestHeadersTest {

  @Nested
  @ExtendWith(LilyExtension.class)
  class InLineSchemaHeader {

    @BeforeAll
    static void beforeAll(LilyTestSupport lily) {
      lily.compileOas(
          """
          openapi: 3.0.2
          paths:
            /pet:
              get:
                operationId: listPets
                parameters:
                  - name: x-limit
                    in: header
                    schema:
                      type: integer
                      format: int32
          """);
    }

    @Test
    void sendSync(LilyTestSupport lily, WireMockRuntimeInfo info) {
      lily.evaluate(
          """
          return {{package}}.Api.newBuilder()
            .uri("{{uri}}")
            .build()
            .everyOperation()
            .listPets()
            .headers(headers -> headers.xLimit(10))
            .sendSync();
          """,
          "uri",
          info.getHttpBaseUrl());

      verify(getRequestedFor(urlEqualTo("/pet")).withHeader("x-limit", equalTo("10")));
    }

    @Test
    void httpRequest(LilyTestSupport lily, WireMockRuntimeInfo info) throws Exception {
      var request =
          lily.evaluate(
              """
          return {{package}}.Api.newBuilder()
            .uri("{{uri}}")
            .build()
            .everyOperation()
            .listPets()
            .headers(headers -> headers.xLimit(10))
            .httpRequest();
          """,
              HttpRequest.class,
              "uri",
              info.getHttpBaseUrl());

      HttpClient.newHttpClient().send(request, BodyHandlers.discarding());

      verify(getRequestedFor(urlEqualTo("/pet")).withHeader("x-limit", equalTo("10")));
    }
  }

  @Nested
  @ExtendWith(LilyExtension.class)
  class ComponentSchemaHeader {
    @BeforeAll
    static void beforeAll(LilyTestSupport lily) {
      lily.compileOas(
          """
          openapi: 3.0.2
          paths:
            /pet:
              get:
                operationId: listPets
                parameters:
                  - name: x-limit
                    in: header
                    schema:
                      $ref: '#/components/schemas/XLimit'
          components:
            schemas:
              XLimit:
                type: integer
                format: int32
          """);
    }

    @Test
    void sendSync(LilyTestSupport lily, WireMockRuntimeInfo info) {
      lily.evaluate(
          """
          return {{package}}.Api.newBuilder()
            .uri("{{uri}}")
            .build()
            .everyOperation()
            .listPets()
            .headers(headers -> headers.xLimit(new {{package}}.XLimit(10)))
            .sendSync();
          """,
          "uri",
          info.getHttpBaseUrl());

      verify(getRequestedFor(urlEqualTo("/pet")).withHeader("x-limit", equalTo("10")));
    }

    @Test
    void httpRequest(LilyTestSupport lily, WireMockRuntimeInfo info) throws Exception {
      var request =
          lily.evaluate(
              """
          return {{package}}.Api.newBuilder()
            .uri("{{uri}}")
            .build()
            .everyOperation()
            .listPets()
            .headers(headers -> headers.xLimit(new {{package}}.XLimit(10)))
            .httpRequest();
          """,
              HttpRequest.class,
              "uri",
              info.getHttpBaseUrl());

      HttpClient.newHttpClient().send(request, BodyHandlers.discarding());

      verify(getRequestedFor(urlEqualTo("/pet")).withHeader("x-limit", equalTo("10")));
    }
  }
}
