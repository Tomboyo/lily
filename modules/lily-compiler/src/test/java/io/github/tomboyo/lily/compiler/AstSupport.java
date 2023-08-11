package io.github.tomboyo.lily.compiler;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import java.util.List;

public class AstSupport {
  /** Used in test cases to create an AstReference whose contents are not under test. */
  public static Fqn fqnPlaceholder() {
    return Fqn.newBuilder().packageName("com.example").typeName("Placeholder").build();
  }

  public static Ast astPlaceholder() {
    return AstClass.of(fqnPlaceholder(), List.of(), "");
  }
}
