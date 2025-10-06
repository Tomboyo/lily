package io.github.tomboyo.lily.compiler.ast;

import java.util.LinkedHashSet;
import java.util.List;

public record AstClass(
    Fqn name, LinkedHashSet<Field> fields, String docstring, LinkedHashSet<Fqn> interfaces)
    implements HasInterface, Ast {
  public static AstClass of(Fqn name, List<Field> fields, String docstring) {
    return new AstClass(name, new LinkedHashSet<>(fields), docstring, new LinkedHashSet<>());
  }

  public static AstClass of(Fqn name, List<Field> fields) {
    return new AstClass(name, new LinkedHashSet<>(fields), "", new LinkedHashSet<>());
  }

  private static <T> LinkedHashSet<T> append(LinkedHashSet<T> set, T el) {
    var tmp = new LinkedHashSet<>(set);
    tmp.add(el);
    return tmp;
  }
}
