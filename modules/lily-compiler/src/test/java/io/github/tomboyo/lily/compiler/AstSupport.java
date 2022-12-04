package io.github.tomboyo.lily.compiler;

import io.github.tomboyo.lily.compiler.ast.Fqn;

public class AstSupport {
  /** Used in test cases to create an AstReference whose contents are not under test. */
  public static Fqn fqnPlaceholder() {
    return Fqn.newBuilder().packageName("com.example").typeName("Placeholder").build();
  }
}
