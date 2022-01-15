package com.github.tomboyo.lily.ast;

import static com.github.tomboyo.lily.icg.Support.capitalCamelCase;

import java.util.List;

public final record AstReference(
    String packageName, String className, List<AstReference> typeParameters) {
  public AstReference(String packageName, String className) {
    this(packageName, className, List.of());
  }

  public AstReference(String packageName, String className, AstReference typeParameter) {
    this(packageName, className, List.of(typeParameter));
  }

  public AstReference(String packageName, String className, List<AstReference> typeParameters) {
    this.packageName = packageName;
    this.className = capitalCamelCase(className);
    this.typeParameters = typeParameters;
  }
}
