package io.github.tomboyo.lily.compiler.ast;

/** ASTs which are addressable by a fully-qualified name (FQN). */
public interface Fqn {
  String packageName();

  String name();
}
