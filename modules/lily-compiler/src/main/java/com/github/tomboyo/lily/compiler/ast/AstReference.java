package com.github.tomboyo.lily.compiler.ast;

import static com.github.tomboyo.lily.compiler.icg.Support.capitalCamelCase;

import java.util.List;

public final record AstReference(String packageName, String name, List<AstReference> typeParameters)
    implements Fqn {
  public AstReference(String packageName, String className) {
    this(packageName, className, List.of());
  }

  public AstReference(String packageName, String className, AstReference typeParameter) {
    this(packageName, className, List.of(typeParameter));
  }

  public AstReference(String packageName, String name, List<AstReference> typeParameters) {
    this.packageName = packageName;
    this.name = capitalCamelCase(name);
    this.typeParameters = typeParameters;
  }
}
