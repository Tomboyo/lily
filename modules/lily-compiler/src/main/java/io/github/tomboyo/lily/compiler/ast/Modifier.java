package io.github.tomboyo.lily.compiler.ast;

/**
 * A Modifier combines with a {@link Definition} for some type to produce a new Definition with an
 * additional characteristic that the original Definition may not have already had, such as an
 * interface implementation.
 */
public sealed interface Modifier extends Ast permits AddInterface {
  Definition modify(Definition definition);
}
