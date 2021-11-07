package com.github.tomboyo.lily.ast.type;

public record AstClassAlias(String name, AstReference aliasedType) implements PackageContents {}
