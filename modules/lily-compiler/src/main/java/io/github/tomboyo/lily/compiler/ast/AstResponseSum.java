package io.github.tomboyo.lily.compiler.ast;

import java.util.Set;

public record AstResponseSum(Fqn fqn, Set<AstReference> members) implements Ast {}
