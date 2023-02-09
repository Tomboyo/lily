package io.github.tomboyo.lily.compiler.feature;

import static io.github.tomboyo.lily.compiler.CompilerSupport.compileOas;
import static io.github.tomboyo.lily.compiler.CompilerSupport.deleteGeneratedSources;
import static io.github.tomboyo.lily.compiler.CompilerSupport.evaluate;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class HttpRequestMethodTest {

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
                  /foo:
                    get:
                      operationId: get-foo
                    post:
                      operationId: post-foo
                """);
  }

  @Test
  void postFoo() {
    actual =
        evaluate(
            """
            return %s.Api.newBuilder()
              .uri("https://example.com/")
              .build()
              .everyOperation()
              .postFoo()
              .httpRequest();
            """
                .formatted(packageName),
            HttpRequest.class);

    assertEquals("POST", actual.method());
  }

  @Test
  void getFoo() {
    actual =
        evaluate(
            """
        return %s.Api.newBuilder()
          .uri("https://example.com/")
          .build()
          .everyOperation()
          .getFoo()
          .httpRequest();
        """
                .formatted(packageName),
            HttpRequest.class);

    assertEquals("GET", actual.method());
  }
}
