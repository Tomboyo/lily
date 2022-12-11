package io.github.tomboyo.lily.compiler.ast;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A package name without a qualified class, like com.example.foo.bar.baz */
public record PackageName(List<String> packageParts) {

  public static PackageName of(String packageName) {
    requireNonNull(packageName);
    if (packageName.isBlank()) {
      throw new IllegalArgumentException("Package name must not be blank");
    }

    return new PackageName(toPackageParts(packageName));
  }

  /**
   * Return the new package created by appending one or more given package name components to this
   * package name. For example, {@code PackageName.of("com.example").resolve("foo.bar").toString()}
   * is {@code "com.example.foo.bar"}.
   */
  public PackageName resolve(String packageName) {
    var copy = new ArrayList<>(packageParts);
    copy.addAll(toPackageParts(packageName));
    return new PackageName(copy);
  }

  /**
   * Return the new package created by appending the given simple name as a single path component.
   * For example, {@code
   * PackageName.of("com.example").resolve(SimpleName.of("FooBarBaz")).toString()} is {@code
   * "com.exmaple.foobarbaz"}.
   */
  public PackageName resolve(SimpleName simpleName) {
    return resolve(simpleName.toString());
  }

  /** Return an array of this package's components, such that a.b.c becomes [a, b, c]. */
  public String[] components() {
    return packageParts.toArray(new String[0]);
  }

  @Override
  public String toString() {
    return joinPackages(packageParts);
  }

  private static List<String> toPackageParts(String first) {
    return Stream.of(first)
        .map(PackageName::withoutLeadingOrTrailingDot)
        .map(String::toLowerCase)
        .flatMap(it -> Arrays.stream(it.split("\\.")))
        .collect(Collectors.toList());
  }

  private static String withoutLeadingOrTrailingDot(String in) {
    if (in.startsWith(".")) {
      in = in.substring(1);
    }

    if (in.endsWith(".")) {
      in = in.substring(0, in.length() - 1);
    }

    return in;
  }

  private String joinPackages(List<String> packageNameParts) {
    return String.join(".", packageNameParts);
  }
}
