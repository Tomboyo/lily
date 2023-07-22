package io.github.tomboyo.lily.compiler.ast;

public sealed interface Ast permits Definition, Modifier {
  Fqn name();
}
