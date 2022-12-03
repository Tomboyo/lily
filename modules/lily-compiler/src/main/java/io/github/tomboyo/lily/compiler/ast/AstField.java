package io.github.tomboyo.lily.compiler.ast;

/** A field of an AstClass. */
public record AstField(AstReference astReference, SimpleName name, String jsonName) {}
