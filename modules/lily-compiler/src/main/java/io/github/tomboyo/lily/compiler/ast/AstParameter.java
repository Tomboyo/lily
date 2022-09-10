package io.github.tomboyo.lily.compiler.ast;

/** A path parameter for an operation */
public record AstParameter(
    SimpleName name, AstParameterLocation location, AstReference astReference) implements Ast {}
