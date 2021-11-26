package com.github.tomboyo.lily.ast.type;

import java.util.Set;

public record AstPackage(String name, Set<AstPackageContents> contents) implements Ast, AstPackageContents {}
