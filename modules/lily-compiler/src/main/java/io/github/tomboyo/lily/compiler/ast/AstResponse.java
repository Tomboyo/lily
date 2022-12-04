package io.github.tomboyo.lily.compiler.ast;

import java.util.LinkedHashSet;

public record AstResponse(Fqn name, LinkedHashSet<Field> fields, Fqn sumTypeName) implements Ast {}
