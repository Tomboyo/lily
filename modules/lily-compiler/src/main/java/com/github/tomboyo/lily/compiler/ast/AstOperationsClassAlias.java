package com.github.tomboyo.lily.compiler.ast;

import java.util.Set;

/**
 * A subset of operations grouped by tag to be aliased together in a file, but not re-defined. Each
 * should delegate to a singleton definition in the {@code operationsSingleton} file.
 */
public record AstOperationsClassAlias(
    String packageName,
    String name,
    AstReference operationsSingleton,
    Set<AstOperation> aliasedOperations)
    implements Ast {}
