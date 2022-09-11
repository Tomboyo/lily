package io.github.tomboyo.lily.compiler.ast;

import java.util.LinkedHashSet;
import java.util.List;

public record AstClass(Fqn name, LinkedHashSet<AstField> fields) implements Ast {
  public static AstClass of(Fqn fqn, List<AstField> fields) {
    return new AstClass(fqn, new LinkedHashSet<>(fields));
  }
}
