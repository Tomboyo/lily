package io.github.tomboyo.lily.compiler.ast;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

// TODO: replace Fqn with this
public record Fqn2(PackageName packageName, ClassName className) {

  public Fqn2(PackageName packageName, ClassName className) {
    this.packageName = requireNonNull(packageName);
    this.className = requireNonNull(className);
  }

  public static Fqn2 of(String packageName, String className) {
    return new Fqn2(PackageName.of(packageName), ClassName.of(className));
  }

  public String fullyQualifiedName() {
    return String.join(".", packageName.toString(), className().upperCamelCase());
  }

  public Path toPath() {
    return Path.of(".", packageName.components())
        .normalize()
        .resolve(className().upperCamelCase() + ".java");
  }

  /** Create a copy of this object but with the given package name */
  public Fqn2 withPackage(String newPackage) {
    return new Fqn2(PackageName.of(newPackage), className);
  }
}
