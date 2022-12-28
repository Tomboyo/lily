package io.github.tomboyo.lily.compiler.ast;

/** The string expansion strategy for a parameter. */
public record ParameterEncoding(Style style, boolean explode) {
  public enum Style {
    SIMPLE,
    FORM,
    UNSUPPORTED
  }

  public static ParameterEncoding simple() {
    return new ParameterEncoding(Style.SIMPLE, false);
  }

  public static ParameterEncoding simpleExplode() {
    return new ParameterEncoding(Style.SIMPLE, true);
  }

  public static ParameterEncoding form() {
    return new ParameterEncoding(Style.FORM, false);
  }

  public static ParameterEncoding formExplode() {
    return new ParameterEncoding(Style.FORM, true);
  }

  public static ParameterEncoding unsupported() {
    return new ParameterEncoding(Style.UNSUPPORTED, false);
  }
}
