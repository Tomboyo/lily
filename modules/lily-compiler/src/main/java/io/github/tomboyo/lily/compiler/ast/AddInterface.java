package io.github.tomboyo.lily.compiler.ast;

/**
 * Modifies an AST such that the resulting type definition implements a desired interface. For
 * example, when we process a OneOf composed schema into a sealed interface, we emit these
 * AddInterface fragments so that all members whose types are already defined elsewhere (i.e. all
 * $ref references) will implement the new interface generated for the OneOf.
 *
 * @param name The FQN of the type which should implement a given interface.
 * @param interfaceName The FQN of the interface to implement.
 */
public record AddInterface(Fqn name, Fqn interfaceName) implements Modifier {
  @Override
  public Definition modify(Definition definition) {
    if (definition instanceof HasInterface x) {
      return x.addInterface(interfaceName);
    } else {
      throw new UnsupportedOperationException(
          "Cannot add interface to type " + definition.getClass());
    }
  }
}
