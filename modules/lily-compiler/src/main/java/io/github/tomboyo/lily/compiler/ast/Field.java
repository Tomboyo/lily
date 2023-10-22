package io.github.tomboyo.lily.compiler.ast;

/** A field of an AstClass. */
public record Field(Fqn astReference, SimpleName name, String jsonName, boolean isMandatory) {}
