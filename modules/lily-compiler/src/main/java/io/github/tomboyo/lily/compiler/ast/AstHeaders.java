package io.github.tomboyo.lily.compiler.ast;

import java.util.List;

public record AstHeaders(Fqn name, List<Field> fields) implements Definition {}
