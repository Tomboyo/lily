package io.github.tomboyo.lily.compiler.ast;

import java.util.Collection;

/** Annotates an Ast that implements interfaces. */
public interface HasInterface {
  Collection<Fqn> interfaces();
}
