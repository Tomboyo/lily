package io.github.tomboyo.lily.compiler.feature;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static io.github.tomboyo.lily.compiler.CompilerSupport.compileOas;
import static io.github.tomboyo.lily.compiler.CompilerSupport.evaluate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.InputStream;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@WireMockTest
class SynchronousRequestTests {

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

                """);
  }

  @BeforeEach
  void beforeEach() {
    stubFor(
        get("/pets")
            .willReturn(
                ok("""
            { "name": "Fido", "age": 5 }
            """)
                    .withHeader("x-my-header", "value")));
  }

  @Test
  void exposeHttpResponses(WireMockRuntimeInfo info) {
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

    assertEquals(200, actual.statusCode());
    assertEquals("value", actual.headers().firstValue("x-my-header").orElseThrow());
    assertTrue(actual.body() instanceof InputStream);
  }
}
