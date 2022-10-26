package io.github.tomboyo.lily.compiler.feature;

import static io.github.tomboyo.lily.compiler.CompilerSupport.compileOas;
import static io.github.tomboyo.lily.compiler.CompilerSupport.deleteGeneratedSources;
import static io.github.tomboyo.lily.compiler.CompilerSupport.evaluate;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class HttpRequestBuildersTest {

  private static String packageName;
  private static HttpRequest actual;

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
                  /foo/{id}:
                    get:
                      operationId: getFoo
                      parameters:
                        - name: id
                          in: path
                          required: true
                          schema:
                            type: string
                        - name: color
                          in: query
                          schema:
                            type: string
                """);

    actual =
        evaluate(
            """
            return %s.Api.newBuilder()
              .uri("https://example.com/")
              .build()
              .allOperations()
              .getFoo()
              .id("1234")
              .color("red")
              .httpRequest();
            """
                .formatted(packageName),
            HttpRequest.class);
  }

  @Test
  void templatesUriWithPathAndQueryParameters() {
    assertEquals("https://example.com/foo/1234?color=red", actual.uri().toString());
  }
}
