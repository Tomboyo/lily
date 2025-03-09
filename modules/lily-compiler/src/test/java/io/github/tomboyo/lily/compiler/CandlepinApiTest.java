package io.github.tomboyo.lily.compiler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.github.tomboyo.lily.compiler.LilyExtension.LilyTestSupport;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Compiles an open-source OpenAPI document for a real system to help unearth bugs. */
@ExtendWith(LilyExtension.class)
class CandlepinApiTest {
  @Test
  void test(LilyTestSupport support) {
    assertDoesNotThrow(
        () ->
            support.compileOas(
                URI.create(
                        "https://raw.githubusercontent.com/candlepin/candlepin/e332838213587b1903e1c6331eaeff5f4912ef7e/api/candlepin-api-spec.yaml")
                    .toURL()));
  }
}
