package io.github.tomboyo.lily.compiler.ast;

import java.util.Set;

/** A collection of tagged operations, where all operations share the same tag. */
public record AstTaggedOperations(Fqn name, Set<AstOperation> operations) implements Definition {}
