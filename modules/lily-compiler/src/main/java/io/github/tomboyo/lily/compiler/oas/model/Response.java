package io.github.tomboyo.lily.compiler.oas.model;

import java.util.Map;

public record Response(Map<String, MediaType> content, Map<String, IHeader> headers)
    implements IResponse {
  public static Response empty() {
    return new Response(Map.of(), Map.of());
  }
}
