package io.github.tomboyo.lily.compiler.ast;

/** The string expansion strategy for a parameter. */
public record AstEncoding(Style style, boolean explode) {
  public enum Style {
    SIMPLE,
    FORM,
    UNSUPPORTED
  }

  public static AstEncoding simple() {
    return new AstEncoding(Style.SIMPLE, false);
  }

  public static AstEncoding simpleExplode() {
    return new AstEncoding(Style.SIMPLE, true);
  }

  public static AstEncoding form() {
    return new AstEncoding(Style.FORM, false);
  }

  public static AstEncoding formExplode() {
    return new AstEncoding(Style.FORM, true);
  }

  public static AstEncoding unsupported() {
    return new AstEncoding(Style.UNSUPPORTED, false);
  }
}
