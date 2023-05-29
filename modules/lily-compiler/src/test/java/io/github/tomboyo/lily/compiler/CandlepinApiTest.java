package io.github.tomboyo.lily.compiler;

import io.github.tomboyo.lily.compiler.LilyExtension.LilyTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class CandlepinApiTest {

  @Test
  void test(@TempDir Path temp) {
    assertDoesNotThrow(() ->
    LilyCompiler.compile(
        URI.create("https://raw.githubusercontent.com/candlepin/candlepin/e332838213587b1903e1c6331eaeff5f4912ef7e/api/candlepin-api-spec.yaml"),
        temp,
        "com.example.candlepin.api",
        true));
  }

}
