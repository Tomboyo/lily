package io.github.tomboyo.lily.compiler.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class FqnTest {

  @Nested
  class FullyQualifiedName {
    @Test
    void fullyQualifiedName() {
      assertEquals("com.example.FooBarBaz", Fqn.of("com.example", "FooBarBaz").toString());
    }
  }

  @Test
  void asPath() {
    assertEquals(
        Path.of("io/github/tomboyo/lily/example/Test.java"),
        Fqn.of("io.github.tomboyo.lily.example", "Test").toPath());
  }
}
