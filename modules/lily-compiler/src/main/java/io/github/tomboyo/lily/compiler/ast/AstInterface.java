package io.github.tomboyo.lily.compiler.ast;

import java.util.List;

public record AstInterface(Fqn name, List<Fqn> permits) implements Definition {}
