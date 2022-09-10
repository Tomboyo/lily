package io.github.tomboyo.lily.compiler.ast;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

/** A fully-qualified name composed of a package name and a simple name, like java.util.String. */
public record Fqn2(PackageName packageName, SimpleName simpleName) {

  public Fqn2(PackageName packageName, SimpleName simpleName) {
    this.packageName = requireNonNull(packageName);
    this.simpleName = requireNonNull(simpleName);
  }

  public static Fqn2 of(String packageName, String className) {
    return new Fqn2(PackageName.of(packageName), SimpleName.of(className));
  }

  public String fullyQualifiedName() {
    return String.join(".", packageName.toString(), simpleName().upperCamelCase());
  }

  public Path toPath() {
    return Path.of(".", packageName.components())
        .normalize()
        .resolve(simpleName().upperCamelCase() + ".java");
  }

  /** Create a copy of this object but with the given package name */
  public Fqn2 withPackage(String newPackage) {
    return new Fqn2(PackageName.of(newPackage), simpleName);
  }
}
