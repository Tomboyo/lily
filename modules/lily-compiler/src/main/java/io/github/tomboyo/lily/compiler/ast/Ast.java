package io.github.tomboyo.lily.compiler.ast;

/** Ast (Abstract Syntax Tree) elements define new Java types. */
public sealed interface Ast permits Definition, Modifier {
  Fqn name();
}
