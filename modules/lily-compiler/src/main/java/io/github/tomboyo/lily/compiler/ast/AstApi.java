package io.github.tomboyo.lily.compiler.ast;

import java.util.Set;

/** The top-level collection of tagged API operations. */
public record AstApi(Fqn name, Set<AstTaggedOperations> taggedOperations) implements Definition {}
