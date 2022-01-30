package com.github.tomboyo.lily.compiler.ast;

import java.util.List;

public record AstClass(String packageName, String name, List<AstField> fields)
    implements Ast, Fqn {}
