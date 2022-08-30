package io.github.tomboyo.lily.compiler.ast;

public enum AstParameterLocation {
  PATH,
  QUERY,
  COOKIE,
  HEADER;

  public static AstParameterLocation fromString(String raw) {
    return switch (raw) {
      case "path" -> PATH;
      case "query" -> QUERY;
      case "cookie" -> COOKIE;
      case "header" -> HEADER;
      default -> throw new IllegalArgumentException("Unrecognized parameter location: " + raw);
    };
  }
}
