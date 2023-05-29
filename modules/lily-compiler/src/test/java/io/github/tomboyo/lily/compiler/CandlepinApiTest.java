package io.github.tomboyo.lily.compiler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.net.URI;
import java.nio.file.Path;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Compiles an open-source OpenAPI document for a real system to help unearth bugs. */
@Disabled("Will not pass until '#74 - Graceful Failure' is finished.")
class CandlepinApiTest {
  @Test
  void test(@TempDir Path temp) {
    assertDoesNotThrow(
        () ->
            LilyCompiler.compile(
                URI.create(
                    "https://raw.githubusercontent.com/candlepin/candlepin/e332838213587b1903e1c6331eaeff5f4912ef7e/api/candlepin-api-spec.yaml"),
                temp,
                "com.example.candlepin.api",
                true));
  }
}
