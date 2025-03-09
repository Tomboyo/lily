package io.github.tomboyo.lily.compiler.oas.model;

import java.util.Map;

/* TODO: MediaType might be nullable, e.g. if the key is set to an empty value. Change to Optional if so. */
public record Response(Map<String, MediaType> content, Map<String, IHeader> headers)
    implements IResponse {
  public static Response empty() {
    return new Response(Map.of(), Map.of());
  }
}
