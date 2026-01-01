package io.github.tomboyo.lily.compiler.ast;

import java.util.Set;

public record AstDirectory(Fqn name, Set<AstTemplate> templates) implements Ast {}
