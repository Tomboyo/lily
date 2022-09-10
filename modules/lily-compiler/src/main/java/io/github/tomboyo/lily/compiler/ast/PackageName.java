package io.github.tomboyo.lily.compiler.ast;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A package name without a qualified class, like com.example.foo.bar.baz */
public class PackageName {

  private final List<String> packageParts;

  private PackageName(List<String> packageParts) {
    this.packageParts = packageParts;
  }

  public static PackageName of(String packageName) {
    requireNonNull(packageName);
    if (packageName.isBlank()) {
      throw new IllegalArgumentException("Package name must not be blank");
    }

    return new PackageName(toPackageParts(packageName));
  }

  /** Return an array of this package's components, such that a.b.c becomes [a, b, c]. */
  public String[] components() {
    return packageParts.toArray(new String[0]);
  }

  @Override
  public String toString() {
    return joinPackages(packageParts);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    PackageName that = (PackageName) o;
    return packageParts.equals(that.packageParts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(packageParts);
  }

  private static List<String> toPackageParts(String first, String... rest) {
    return Stream.concat(Stream.of(first), Arrays.stream(rest))
        .filter(it -> !it.isBlank())
        .map(PackageName::withoutLeadingOrTrailingDot)
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
