package io.github.tomboyo.lily.compiler.ast;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

/** A fully-qualified name composed of a package name and a simple name, like java.util.String. */
public record Fqn(PackageName packageName, SimpleName simpleName) {

  public Fqn(PackageName packageName, SimpleName simpleName) {
    this.packageName = requireNonNull(packageName);
    this.simpleName = requireNonNull(simpleName);
  }

  public static Fqn of(String packageName, String className) {
    return new Fqn(PackageName.of(packageName), SimpleName.of(className));
  }

  public static Fqn of(PackageName packageName, SimpleName className) {
    return new Fqn(packageName, className);
  }

  /**
   * Renders the fully qualified class name represented by this instance, like {@code
   * "java.util.String"}
   */
  @Override
  public String toString() {
    return String.join(".", packageName.toString(), simpleName().upperCamelCase());
  }

  public Path toPath() {
    return Path.of(".", packageName.components())
        .normalize()
        .resolve(simpleName().upperCamelCase() + ".java");
  }

  /** Create a copy of this object but with the given package name */
  public Fqn withPackage(String newPackage) {
    return new Fqn(PackageName.of(newPackage), simpleName);
  }
}
