package io.github.tomboyo.lily.compiler.ast;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.tomboyo.lily.compiler.icg.Support.capitalCamelCase;
import static java.util.Objects.requireNonNull;

//TODO: replace Fqn with this
public class Fqn2 {

  private final List<String> packageParts;
  private final List<String> nameParts;

  private Fqn2(List<String> packageParts, List<String> nameParts) {
    this.packageParts = packageParts;
    this.nameParts = nameParts;
  }

  public static class SimpleName {

    private final List<String> nameParts;

    private SimpleName(List<String> nameParts) {
      this.nameParts = nameParts;
    }

    public String upperCamelCase() {
      return nameParts.stream()
          .map(this::upperCase)
          .collect(Collectors.joining(""));
    }

    public String lowerCamelCase() {
      var first = nameParts.get(0).toLowerCase();
      var rest = nameParts.stream()
          .skip(1)
          .map(this::upperCase)
          .collect(Collectors.joining(""));
      return first + rest;
    }

    private String upperCase(String word) {
      return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
    }
  }

  public static class Builder {
    private List<String> packageNameParts;
    private String name;

    private Builder() {}

    public Builder withPackage(String first, String... rest) {
      packageNameParts = toPackageParts(first, rest);
      return this;
    }

    public Builder withClassName(String name) {
      this.name = name;
      return this;
    }

    private static final Pattern STARTS_WITH_DIGIT = Pattern.compile("^\\d.*");

    public Fqn2 build() {
      requireNonNull(name, "Names cannot be null");

      if (name.isBlank()) {
        throw new IllegalArgumentException("Names cannot be blank");
      }

      if(STARTS_WITH_DIGIT.matcher(name).matches()) {
        throw new IllegalArgumentException("Names cannot begin with digits");
      }

      return new Fqn2(packageNameParts, splitName(name));
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
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static Fqn2 of(String packageName, String className) {
    return Fqn2.newBuilder().withPackage(packageName).withClassName(className).build();
  }

  public SimpleName className() {
    return new SimpleName(nameParts);
  }

  public String packageName() {
    return joinPackages(packageParts);
  }

  public String fullyQualifiedName() {
    return joinPackages(packageParts) + "." + className().upperCamelCase();
  }

  public Path asPath() {
    return Path.of(".", packageParts.toArray(new String[]{}))
        .normalize()
        .resolve(className().upperCamelCase() + ".java");
  }

  /** Create a copy of this object but with the given package name */
  public Fqn2 withPackage(String newPackage) {
    return new Fqn2(toPackageParts(newPackage), nameParts);
  }

  private static String joinPackages(List<String> packageNameParts) {
    return packageNameParts.stream()
        .collect(Collectors.joining("."));
  }

  private static List<String> toPackageParts(String first, String... rest) {
    return Stream.concat(
            Stream.of(first),
            Arrays.stream(rest))
        .filter(it -> !it.isBlank())
        .map(Builder::withoutLeadingOrTrailingDot)
        .collect(Collectors.toList());
  }

  private static final Pattern CAMEL_CASE_PATTERN = Pattern
      .compile("[0-9a-z]+|([A-Z](([A-Z]*(?![a-z]))|[0-9a-z]*))");

  private static List<String> splitName(String name) {
    if (name.contains("-")) {
      // kebab case
      return Arrays.asList(name.split("-"));
    } else if (name.contains("_")) {
      // snake case
      return Arrays.asList(name.split("_"));
    } else {
      var matcher = CAMEL_CASE_PATTERN.matcher(name);
      return matcher
          .results()
          .map(MatchResult::group)
          .filter(it -> !it.isBlank())
          .collect(Collectors.toList());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Fqn2 fqn2 = (Fqn2) o;
    return packageParts.equals(fqn2.packageParts) && nameParts.equals(fqn2.nameParts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(packageParts, nameParts);
  }
}
