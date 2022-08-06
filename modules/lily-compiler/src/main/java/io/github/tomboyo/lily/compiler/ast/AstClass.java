package io.github.tomboyo.lily.compiler.ast;

import java.util.LinkedHashSet;
import java.util.List;

public record AstClass(String packageName, String name, LinkedHashSet<AstField> fields)
    implements Ast, Fqn {
  public static AstClass of(String packageName, String name, List<AstField> fields) {
    return new AstClass(packageName, name, new LinkedHashSet<>(fields));
  }
}
