package io.github.tomboyo.lily.compiler.ast;

public sealed interface Ast
    permits AstApi,
        AstClass,
        AstClassAlias,
        AstOperation,
        AstParameter,
        AstResponseSum,
        AstTaggedOperations {}
