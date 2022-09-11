package io.github.tomboyo.lily.compiler.ast;

import java.util.List;

public record AstReference(Fqn name, List<AstReference> typeParameters, boolean isArray) {
  public static AstReference newTypeRef(Fqn name, List<AstReference> typeParameters) {
    return new AstReference(name, typeParameters, false);
  }
}
