package io.github.tomboyo.lily.compiler.ast;

/** A field of an AstClass. */
public record AstField(AstReference astReference, String name) {
  public AstField(AstReference astReference, String name) {
    this.astReference = astReference;
    // to fieldNameCase
    this.name = name.substring(0, 1).toLowerCase() + name.substring(1);
  }
}
