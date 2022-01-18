package com.github.tomboyo.lily.ast;

import java.util.Set;

/** A collection of operation definitions, to be rendered together in one file. */
public record AstOperationsClass(String packageName, String name, Set<AstOperation> operations)
    implements Ast {}
