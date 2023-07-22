package io.github.tomboyo.lily.compiler.ast;

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
