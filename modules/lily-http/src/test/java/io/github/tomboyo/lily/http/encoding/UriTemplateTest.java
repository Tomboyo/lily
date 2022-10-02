package io.github.tomboyo.lily.http.encoding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.tomboyo.lily.http.UriTemplate;
import io.github.tomboyo.lily.http.UriTemplateException;
import java.net.URI;
import org.junit.jupiter.api.Test;

public class UriTemplateTest {
  @Test
  void requiresAllParameters() {
    assertThrows(
        UriTemplateException.class,
        () ->
            UriTemplate.of("https://example.com/pets/{petId}/foo/{foo}/")
                .bind("petId", "5")
                .toURI());
  }

  @Test
  void interpolatesParameters() {
    var uri =
        UriTemplate.of("https://example.com/pets/{petId}/foo/{foo}")
            .bind("petId", "5")
            .bind("foo", "f%35oo")
            .toURI();

    assertEquals(uri, URI.create("https://example.com/pets/5/foo/f%35oo"));
  }

  @Test
  void removesExtraneousSlashes() {
    var uri = UriTemplate.of("https://example.com/pets/", "/foo/", "/bar/").toURI();

    assertEquals(uri, URI.create("https://example.com/pets/foo/bar"));
  }

  @Test
  void unbind() {
    var uri =
        UriTemplate.of("https://example.com/foo/{bar}")
            .bind("bar", "?key=value")
            .unbind("bar")
            .toURI()
            .toString();

    assertEquals(uri, "https://example.com/foo/");
  }
}
