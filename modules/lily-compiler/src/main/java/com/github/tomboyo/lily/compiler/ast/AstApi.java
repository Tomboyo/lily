package com.github.tomboyo.lily.compiler.ast;

import java.util.Set;

/** The top-level collection of tagged API operations. */
public record AstApi(String packageName, String name, Set<AstTaggedOperations> taggedOperations)
    implements Ast, Fqn {}
