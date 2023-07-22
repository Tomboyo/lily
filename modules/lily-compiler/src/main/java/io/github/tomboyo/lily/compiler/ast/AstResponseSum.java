package io.github.tomboyo.lily.compiler.ast;

import java.util.Map;

/**
 * A sum type over all operation responses. For example, GetFooResponse with members GetFoo200 and
 * GetFoo404.
 */
public record AstResponseSum(Fqn name, Map<String, Fqn> statusCodeToMember) implements Definition {}
