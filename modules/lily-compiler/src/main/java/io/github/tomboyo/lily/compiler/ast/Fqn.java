package io.github.tomboyo.lily.compiler.ast;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A fully-qualified name of a type consisting of its package name, type name, and type parameters,
 * such as {@code java.lang.String} or {@code java.util.List<java.lang.String>}.
 */
public record Fqn(PackageName packageName, SimpleName typeName, List<Fqn> typeParameters) {

  /**
   * Get a Fqn builder initialized to create Fqns in the {@code java.lang} package with no type
   * parameters unless overridden.
   */
  public static Builder newBuilder() {
    return new Builder();
  }

  /** Get a Fqn builder initialized to create copies of the given template until overridden. */
  public static Builder newBuilder(Fqn template) {
    return new Builder()
        .packageName(template.packageName)
        .typeName(template.typeName)
        .typeParameters(template.typeParameters);
  }

  public static Builder newBuilder(PackageName packageName, SimpleName typeName) {
    return newBuilder(packageName.toString(), typeName.toString());
  }

  /** Get a Fqn builder initialized with the given package and type names. */
  public static Builder newBuilder(String packageName, String typeName) {
    return new Builder().packageName(packageName).typeName(typeName);
  }

  /** Equivalent to {@link #toFqString()}. */
  @Override
  public String toString() {
    return toFqString();
  }

  /**
   * Renders the fully qualified name represented by this instance without type parameters, like
   * {@code "java.util.List"}
   */
  public String toFqString() {
    return String.join(".", packageName.toString(), typeName().upperCamelCase());
  }

  /**
   * Returns the fully qualified and parameterized name represented by this instance, like {@code
   * java.util.List<java.util.String>}.
   */
  public String toFqpString() {
    if (typeParameters().isEmpty()) {
      return toFqString();
    } else {
      return toFqString()
          + "<%s>"
              .formatted(
                  typeParameters.stream().map(Fqn::toFqpString).collect(Collectors.joining(", ")));
    }
  }

  /**
   * Convert this Fqn to a relative source file path. For example, the Fqn for {@code
   * java.lang.String} maps to a file at {@code ./java/lang/String.java}.
   */
  public Path toPath() {
    return Path.of(".", packageName.components())
        .normalize()
        .resolve(typeName().upperCamelCase() + ".java");
  }

  /**
   * Convert this Fqn into a package. Given {@code com.example.FooBar }, returns the package {@code
   * com.example.foobar}.
   */
  public PackageName toPackage() {
    return packageName.resolve(typeName);
  }

  /**
   * If this FQN describes a List, return an Optional of the List element's FQN, or else an empty
   * optional.
   */
  public Optional<Fqn> listType() {
    if (packageName.toString().equalsIgnoreCase("java.util")
        && typeName.toString().equalsIgnoreCase("List")) {
      return Optional.of(typeParameters.get(0));
    } else {
      return Optional.empty();
    }
  }

  public static class Builder {
    private PackageName packageName = PackageName.of("java.lang");
    private SimpleName typeName;
    private List<Fqn> typeParameters = List.of();

    public Builder packageName(PackageName packageName) {
      this.packageName = packageName;
      return this;
    }

    public Builder packageName(String packageName) {
      this.packageName = PackageName.of(packageName);
      return this;
    }

    public Builder typeName(SimpleName typeName) {
      this.typeName = typeName;
      return this;
    }

    public Builder typeName(String typeName) {
      this.typeName = SimpleName.of(typeName);
      return this;
    }

    public Builder typeParameters(List<Fqn> typeParameters) {
      this.typeParameters = typeParameters;
      return this;
    }

    public Fqn build() {
      return new Fqn(
          requireNonNull(packageName), requireNonNull(typeName), requireNonNull(typeParameters));
    }
  }
}
