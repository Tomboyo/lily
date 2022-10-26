package io.github.tomboyo.lily.compiler.feature;

import static io.github.tomboyo.lily.compiler.CompilerSupport.deleteGeneratedSources;
import static io.github.tomboyo.lily.compiler.CompilerSupport.evaluate;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.tomboyo.lily.compiler.CompilerSupport;
import java.net.http.HttpClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The generated API consumes an HttpClient when constructed. This client can be accessed at any
 * time from any operation in the API, letting users dip into the lower-level API whenever necessary
 * without restructuring code.
 */
public class HttpClientHolderTest {

  static String packageName;

  @BeforeAll
  static void beforeAll() throws Exception {
    packageName =
        CompilerSupport.compileOas(
            """
            openapi: 3.0.2
            paths:
              /foo:
                get:
                  operationId: GetFoo
            """);
  }

  @AfterAll
  static void afterAll() throws Exception {
    deleteGeneratedSources();
  }

  @Test
  void apiExposesGivenClient() {
    var actual =
        evaluate(
            """
            var original = java.net.http.HttpClient.newBuilder().build();
            var actual = %s.Api.newBuilder()
              .uri("https://example.com/")
              .httpClient(original)
              .build()
              .httpClient();
            return original == actual;
            """
                .formatted(packageName),
            Boolean.class);

    assertTrue(actual);
  }

  @Test
  void apiDefaultsHttpClient() throws Exception {
    var actual =
        evaluate(
            """
        return %s.Api.newBuilder()
          .uri("https://example.com/")
          .build()
          .httpClient();
        """
                .formatted(packageName),
            HttpClient.class);

    assertNotNull(actual);
  }
}
