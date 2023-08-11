package io.github.tomboyo.lily.compiler.ast;

import java.util.Collection;

/** Annotates a Definition that implements interfaces. */
public interface HasInterface {

  /**
   * Return a new definition by adding the given interface name to this definition's implements
   * clause.
   */
  Definition addInterface(Fqn interfaceName);

  Collection<Fqn> interfaces();
}
