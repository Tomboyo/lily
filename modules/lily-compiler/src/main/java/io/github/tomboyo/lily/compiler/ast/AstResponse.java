package io.github.tomboyo.lily.compiler.ast;

import java.util.LinkedHashSet;

public record AstResponse(Fqn name, LinkedHashSet<AstField> fields, Fqn sumTypeName)
    implements Ast {}
