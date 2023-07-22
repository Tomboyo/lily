package io.github.tomboyo.lily.compiler.ast;

/** Annotates a Definition that implements interfaces. */
public interface HasInterface {

  /**
   * Return a new definition by adding the given interface name to this definition's implements
   * clause.
   */
  Definition addInterface(Fqn interfaceName);
}
