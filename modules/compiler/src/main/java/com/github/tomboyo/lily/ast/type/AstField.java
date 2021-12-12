package com.github.tomboyo.lily.ast.type;

/** A field of an AstClass. */
public record AstField(AstReference astReference, String name) {
  public AstField(AstReference astReference, String name) {
    this.astReference = astReference;
    // to fieldNameCase
    this.name = name.substring(0, 1).toLowerCase() + name.substring(1);
  }
}
