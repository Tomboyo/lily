package io.github.tomboyo.lily.compiler.ast;

import java.util.List;

public record AstResponseSum(Fqn name, List<Fqn> members) implements Ast {}
