package com.github.tomboyo.lily.compiler.ast;

import java.util.Set;

/**
 * Corresponds to the API entrypoint, an "object mother" for each generated AstOperationsClassAlias.
 */
public record AstApi(String packageName, String name, Set<AstReference> astOperationsAliases)
    implements Ast, Fqn {}
