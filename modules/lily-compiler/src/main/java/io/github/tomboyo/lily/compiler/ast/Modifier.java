package io.github.tomboyo.lily.compiler.ast;

public sealed interface Modifier extends Ast permits AddInterface {
  Definition modify(Definition definition);
}
