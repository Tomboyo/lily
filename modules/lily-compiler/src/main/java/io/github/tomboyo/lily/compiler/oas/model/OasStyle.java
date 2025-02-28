package io.github.tomboyo.lily.compiler.oas.model;

public enum OasStyle {
  MATRIX,
  LABEL,
  SIMPLE,
  FORM,
  SPACE_DELIMITED,
  PIPE_DELIMITED,
  DEEP_OBJECT,
  UNKNOWN;

  public static OasStyle forString(String style) {
    return switch (style) {
      case "matrix" -> MATRIX;
      case "label" -> LABEL;
      case "simple" -> SIMPLE;
      case "form" -> FORM;
      case "spaceDelimited" -> SPACE_DELIMITED;
      case "pipeDelimited" -> PIPE_DELIMITED;
      case "deepObject" -> DEEP_OBJECT;
      default -> UNKNOWN;
    };
  }
}
