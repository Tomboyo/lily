package com.github.tomboyo.lily.ast.type;

import java.util.List;

import static com.github.tomboyo.lily.ast.Support.toClassCase;

public final record AstReference(String packageName, String className, List<AstReference> typeParameters) {
    public AstReference(String packageName, String className) {
        this(packageName, className, List.of());
    }

    public AstReference(String packageName, String className, List<AstReference> typeParameters) {
        this.packageName = packageName;
        this.className = toClassCase(className);
        this.typeParameters = typeParameters;
    }
}
