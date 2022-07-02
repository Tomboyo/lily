package io.github.tomboyo.lily.compiler.ast;

import java.util.Set;

/** A collection of tagged operations, where all operations share the same tag. */
public record AstTaggedOperations(String packageName, String name, Set<AstOperation> operations)
    implements Ast, Fqn {}
