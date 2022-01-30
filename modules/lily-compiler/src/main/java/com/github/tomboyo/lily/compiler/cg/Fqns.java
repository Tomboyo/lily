package com.github.tomboyo.lily.compiler.cg;

import static com.github.tomboyo.lily.compiler.icg.Support.capitalCamelCase;
import static java.lang.String.join;

import com.github.tomboyo.lily.compiler.ast.Fqn;
import java.nio.file.Path;

/** Code-generation helper functions that apply to FQN ASTs (i.e. addressable or nameable ASTs). */
public class Fqns {
  private Fqns() {}

  public static String fqn(Fqn fqn) {
    return join(".", fqn.packageName(), capitalCamelCase(fqn.name()));
  }

  public static Path filePath(Fqn fqn) {
    return Path.of(".", fqn.packageName().split("\\."))
        .normalize()
        .resolve(capitalCamelCase(fqn.name()) + ".java");
  }
}
