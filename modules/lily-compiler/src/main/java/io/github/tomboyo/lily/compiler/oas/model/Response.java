package io.github.tomboyo.lily.compiler.oas.model;

import java.util.Map;

public record Response(OMap<String, MediaType> content, Map<String, IHeader> headers)
    implements IResponse {
  public static Response empty() {
    return new Response(OMap.of(), Map.of());
  }
}
