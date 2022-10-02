package io.github.tomboyo.lily.http.encoding;

import static io.github.tomboyo.lily.http.encoding.Encoding.Modifiers.EXPLODE;
import static io.github.tomboyo.lily.http.encoding.Encoding.form;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.tomboyo.lily.http.UriTemplate;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class UriTemplateTest {
  @Test
  void bindInterpolatesGivenStringsExactly() {
    var uri =
        UriTemplate.of("https://example.com/pets/{petId}/").bind("petId", "?").toURI().toString();

    assertEquals("https://example.com/pets/?/", uri);
  }

  @Test
  void bindInterpolatesUsingEncoders() {
    var uri =
        UriTemplate.of("https://example.com/pets/{colors}")
            .bind("colors", Map.of("key", "value?"), form(EXPLODE))
            .toURI()
            .toString();

    assertEquals("https://example.com/pets/?key=value%3F", uri);
  }

  @Nested
  class Of {
    @Test
    void removesExtraneousSlashes() {
      var uri = UriTemplate.of("https://example.com/pets/", "/foo/", "/bar/").toURI().toString();

      assertEquals("https://example.com/pets/foo/bar/", uri);
    }

    static Stream<Arguments> parameters() {
      return Stream.of(
          arguments("https://example.com/foo", "https://example.com/foo", null),
          arguments("https://example.com/foo", "https://example.com", new String[] {"foo"}),
          arguments("https://example.com/foo/", "https://example.com/foo/", null),
          arguments("https://example.com/foo/", "https://example.com", new String[] {"foo/"}));
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void preservesTrailingSlashOrLackThereof(String expected, String first, String[] rest) {
      var uri = UriTemplate.of(first, rest);
      assertEquals(expected, uri.toURI().toString());
    }
  }

  @Test
  void unbindRemovesBindings() {
    var uri =
        UriTemplate.of("https://example.com/foo/{bar}")
            .bind("bar", "?key=value")
            .unbind("bar")
            .toURI()
            .toString();

    assertEquals("https://example.com/foo/", uri);
  }

  @Test
  void unboundParametersAreLeftBlank() {
    var uri = UriTemplate.of("https://example.com/{foo}/{?bar}").toURI().toString();

    assertEquals("https://example.com//", uri);
  }
}
