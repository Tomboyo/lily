package io.github.tomboyo.lily.compiler.ast;

import java.util.LinkedHashSet;
import java.util.List;

public record AstClass(Fqn name, LinkedHashSet<Field> fields) implements Ast {
  public static AstClass of(Fqn name, List<Field> fields) {
    return new AstClass(name, new LinkedHashSet<>(fields));
  }
}
