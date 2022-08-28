package io.github.tomboyo.lily.compiler;

import static io.github.tomboyo.lily.compiler.icg.StdlibAstReferences.astBoolean;

import io.github.tomboyo.lily.compiler.ast.AstReference;

public class AstSupport {
  /** Used in test cases to create an AstReference whose contents are not under test. */
  public static AstReference astReferencePlaceholder() {
    return astBoolean();
  }
}
