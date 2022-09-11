package io.github.tomboyo.lily.compiler.ast;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A name without a package qualifier, like String or myField.
 *
 * <p>A simple name may be constructed from raw strings in camelCase (and PascalCase), kebab-case,
 * and snake_case. When formatted to a string using {@link #lowerCamelCase()} or {@link
 * #upperCamelCase()}, word boundaries from the raw input are respected.
 */
public class SimpleName {

  private final String raw;
  private final List<String> nameParts;

  private SimpleName(String raw, List<String> nameParts) {
    this.raw = raw;
    this.nameParts = nameParts;
  }

  public static SimpleName of(String name) {
    requireNonNull(name);

    if (name.isBlank()) {
      throw new IllegalArgumentException("Simple name must not be blank");
    }

    if (startsWithDigit(name)) {
      throw new IllegalArgumentException("Simple name must not start with a digit");
    }

    return new SimpleName(name, splitName(name));
  }

  public String upperCamelCase() {
    return nameParts.stream().map(this::upperCase).collect(Collectors.joining(""));
  }

  public String lowerCamelCase() {
    var first = nameParts.get(0).toLowerCase();
    var rest = nameParts.stream().skip(1).map(this::upperCase).collect(Collectors.joining(""));
    return first + rest;
  }

  /**
   * Return a new SimpleName formed by appending one or more 'words' to this name in any supported
   * naming style. For example, {@code SimpleName.of("foo-bar").resolve("BigBang").upperCamelCase()}
   * is {@code "FooBarBigBang}. Note that any name components resolved in this way do NOT count as
   * being part of the "raw" name retrieved with {@link #raw()}.
   */
  public SimpleName resolve(String parts) {
    requireNonNull(parts);
    var copy = new ArrayList<>(nameParts);
    copy.addAll(splitName(parts));
    return new SimpleName(raw, copy);
  }

  /**
   * Get the unmodified string this SimpleName was instantiated with. If any name parts were
   * appended using {@link #resolve(String)}, they are not included in the return value.
   */
  public String raw() {
    return raw;
  }

  /**
   * Render this name as a string in an arbitrary, unspecified format. Suitable for debugging and
   * situations where the format is irrelevant.
   */
  @Override
  public String toString() {
    return upperCamelCase();
  }

  /**
   * True if the other simple name is composed of the same words regardless of case. In particular,
   * the 'raw' form of the name (see {@link #raw()}) is ignored.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SimpleName simpleName = (SimpleName) o;
    return nameParts.equals(simpleName.nameParts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(nameParts);
  }

  private static final Pattern CAMEL_CASE_PATTERN =
      Pattern.compile("[0-9a-z]+|([A-Z](([A-Z]*(?![a-z]))|[0-9a-z]*))");

  private static List<String> splitName(String name) {
    return streamParts(name).map(String::toLowerCase).collect(Collectors.toList());
  }

  private static Stream<String> streamParts(String name) {
    if (name.contains("-")) {
      // kebab case
      return Arrays.stream(name.split("-"));
    } else if (name.contains("_")) {
      // snake case
      return Arrays.stream(name.split("_"));
    } else {
      var matcher = CAMEL_CASE_PATTERN.matcher(name);
      return matcher.results().map(MatchResult::group).filter(it -> !it.isBlank());
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
