package io.github.tomboyo.lily.compiler.ast;

import java.util.Set;

/** The top-level collection of tagged API operations. */
public record AstApi(Fqn2 fqn, Set<AstTaggedOperations> taggedOperations) implements Ast {}
