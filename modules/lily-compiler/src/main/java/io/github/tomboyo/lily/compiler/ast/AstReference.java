package io.github.tomboyo.lily.compiler.ast;

import java.util.List;

public record AstReference(
    String packageName, String name, List<AstReference> typeParameters, boolean isProvidedType)
    implements Fqn {}
