package com.github.tomboyo.lily.ast.type;

import java.util.List;

public record AstClass(String packageName, String name, List<AstField> fields) implements Ast {}
