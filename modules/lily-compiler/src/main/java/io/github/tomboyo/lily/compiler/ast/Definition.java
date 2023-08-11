package io.github.tomboyo.lily.compiler.ast;

/**
 * A Definition describes a new Java type. While a Definition may be modified by a {@link Modifier},
 * it is not necessary to do so in order to generate source code for a new type.
 */
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
