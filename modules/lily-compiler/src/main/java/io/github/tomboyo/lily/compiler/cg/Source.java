package io.github.tomboyo.lily.compiler.cg;

import io.github.tomboyo.lily.compiler.ast.Fqn;
import java.nio.file.Path;

public record Source(Path relativePath, String fqn, String contents) {
  public Source(Fqn fqn, String contents) {
    this(fqn.toPath(), fqn.toFqString(), contents);
  }
}
