package io.github.tomboyo.lily.compiler.ast;

/** A path parameter for an operation */
public record AstParameter(String name, AstReference astReference) implements Ast {}
