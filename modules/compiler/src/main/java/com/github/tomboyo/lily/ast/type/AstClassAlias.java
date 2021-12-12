package com.github.tomboyo.lily.ast.type;

/**
 * A type alias, or alternative name, for some other type.
 *
 * <p>There is no first-class Java representation of a type alias, but it could be rendered either
 * by substituting for the referent type, or as a "wrapper" class.
 */
public record AstClassAlias(String packageName, String name, AstReference aliasedType)
    implements Ast {}
