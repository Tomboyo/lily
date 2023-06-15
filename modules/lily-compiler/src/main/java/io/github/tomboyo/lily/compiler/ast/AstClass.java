package io.github.tomboyo.lily.compiler.ast;

import java.util.LinkedHashSet;
import java.util.List;

public record AstClass(Fqn name, LinkedHashSet<Field> fields, String docstring) implements Ast {
  public static AstClass of(Fqn name, List<Field> fields, String docstring) {
    return new AstClass(name, new LinkedHashSet<>(fields), docstring);
  }

  public static AstClass of(Fqn name, List<Field> fields) {
    return new AstClass(name, new LinkedHashSet<>(fields), "");
  }
}
