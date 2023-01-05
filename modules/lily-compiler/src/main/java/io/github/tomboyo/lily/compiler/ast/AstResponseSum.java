package io.github.tomboyo.lily.compiler.ast;

import java.util.Map;

public record AstResponseSum(Fqn name, Map<String, Fqn> statusCodeToMember) implements Ast {}
