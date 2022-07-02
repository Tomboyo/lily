package io.github.tomboyo.lily.compiler.ast;

public sealed interface Ast
    permits AstClass, AstClassAlias, AstApi, AstTaggedOperations, AstOperation {}
