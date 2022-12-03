package io.github.tomboyo.lily.compiler.ast;

/** A path parameter for an operation */
public record AstParameter(
    SimpleName name,
    String apiName,
    AstParameterLocation location,
    AstEncoding encoding,
    AstReference astReference)
    implements Ast {}
