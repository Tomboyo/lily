package io.github.tomboyo.lily.http.encoding;

import static io.github.tomboyo.lily.http.encoding.Encoding.simple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.tomboyo.lily.http.UriTemplate;
import io.github.tomboyo.lily.http.UriTemplateException;
import java.net.URI;
import org.junit.jupiter.api.Test;

public class UriTemplateTest {
  @Test
  public void requiresAllParameters() {
    assertThrows(
        UriTemplateException.class,
        () ->
            UriTemplate.of("https://example.com/pets/{petId}/foo/{foo}/")
                .bind("petId", simple(5))
                .toURI());
  }

  @Test
  public void interpolatesParameters() {
    var uri =
        UriTemplate.of("https://example.com/pets/{petId}/foo/{foo}")
            .bind("petId", simple(5))
            .bind("foo", simple("f%o/o!"))
            .toURI();

    assertEquals(uri, URI.create("https://example.com/pets/5/foo/f%25o%2Fo%21"));
  }

  @Test
  public void removesExtraneousSlashes() throws Exception {
    var uri = UriTemplate.of("https://example.com/pets/", "/foo/", "/bar/").toURI();

    assertEquals(uri, URI.create("https://example.com/pets/foo/bar"));
  }
}
