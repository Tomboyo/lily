package io.github.tomboyo.lily.compiler.ast;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** A name without a package qualifier, like String or myField. */
public class ClassName {

  private final List<String> nameParts;

  private ClassName(List<String> nameParts) {
    this.nameParts = nameParts;
  }

  public static ClassName of(String name) {
    requireNonNull(name);

    if (name.isBlank()) {
      throw new IllegalArgumentException("Class name must not be blank");
    }

    if (startsWithDigit(name)) {
      throw new IllegalArgumentException("Class name must not start with a digit");
    }

    return new ClassName(splitName(name));
  }

  public String upperCamelCase() {
    return nameParts.stream().map(this::upperCase).collect(Collectors.joining(""));
  }

  public String lowerCamelCase() {
    var first = nameParts.get(0).toLowerCase();
    var rest = nameParts.stream().skip(1).map(this::upperCase).collect(Collectors.joining(""));
    return first + rest;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ClassName className = (ClassName) o;
    return nameParts.equals(className.nameParts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nameParts);
  }

  private static final Pattern CAMEL_CASE_PATTERN =
      Pattern.compile("[0-9a-z]+|([A-Z](([A-Z]*(?![a-z]))|[0-9a-z]*))");

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

  private String upperCase(String word) {
    return word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase();
  }

  private static final Pattern STARTS_WITH_DIGIT = Pattern.compile("^\\d.*");

  static boolean startsWithDigit(String name) {
    return STARTS_WITH_DIGIT.matcher(name).matches();
  }
}
