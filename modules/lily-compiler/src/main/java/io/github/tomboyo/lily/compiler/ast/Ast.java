package io.github.tomboyo.lily.compiler.ast;

public sealed interface Ast
    permits AstApi,
        AstClass,
        AstClassAlias,
        AstHeaders,
        AstOperation,
        AstResponse,
        AstResponseSum,
        AstTaggedOperations {
  Fqn name();
}
