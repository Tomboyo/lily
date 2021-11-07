package com.github.tomboyo.lily.ast.type;

import java.util.Set;

public record NewPackage(String name, Set<PackageContents> contents) implements Ast, PackageContents {}
