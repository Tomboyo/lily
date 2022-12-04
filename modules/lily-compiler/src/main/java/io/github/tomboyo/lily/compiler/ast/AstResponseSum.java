package io.github.tomboyo.lily.compiler.ast;

import java.util.Set;

public record AstResponseSum(Fqn name, Set<Fqn> members) implements Ast {}
