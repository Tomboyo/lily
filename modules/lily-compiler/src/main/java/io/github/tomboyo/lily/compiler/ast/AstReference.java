package io.github.tomboyo.lily.compiler.ast;

import java.util.List;

public record AstReference(
    Fqn2 name, List<AstReference> typeParameters, boolean isProvidedType, boolean isArray) {
  public static AstReference newTypeRef(Fqn2 name, List<AstReference> typeParameters) {
    return new AstReference(name, typeParameters, false, false);
  }

  public static AstReference providedTypeRef(Fqn2 name, List<AstReference> typeParameters) {
    return new AstReference(name, typeParameters, true, false);
  }
}
