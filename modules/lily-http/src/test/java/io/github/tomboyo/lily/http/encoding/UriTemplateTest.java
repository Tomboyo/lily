package io.github.tomboyo.lily.http.encoding;

import static io.github.tomboyo.lily.http.encoding.Encoding.Modifiers.EXPLODE;
import static io.github.tomboyo.lily.http.encoding.Encoding.form;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.tomboyo.lily.http.UriTemplate;
import java.util.Map;
import org.junit.jupiter.api.Test;

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

  @Test
  void unboundParametersAreLeftBlank() {
    var uri = UriTemplate.of("https://example.com/{foo}/{?bar}").toURI().toString();

    assertEquals("https://example.com//", uri);
  }
}
