package io.github.tomboyo.lily.compiler.ast;

public sealed interface Definition extends Ast
    permits AstApi,
        AstClass,
        AstClassAlias,
        AstHeaders,
        AstInterface,
        AstOperation,
        AstResponse,
        AstResponseSum,
        AstTaggedOperations {}
