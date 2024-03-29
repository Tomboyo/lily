package io.github.tomboyo.lily.compiler.ast;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PackageNameTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "com.example",
        ".com.example",
        "com.example.",
        ".com.example.",
        "COM.EXAMPLE",
        "cOm.eXaMpLe"
      })
  void toString(String input) {
    assertEquals("com.example", PackageName.of(input).toString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"com.example", ".com.example", "com.example.", ".com.example."})
  void append(String input) {
    assertEquals("foo.bar.com.example", PackageName.of("foo.bar").resolve(input).toString());
  }

  @ParameterizedTest
  @NullAndEmptySource
  void invalidInput(String input) {
    assertThrows(Exception.class, () -> PackageName.of(input));
  }
}
