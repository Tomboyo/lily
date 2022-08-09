package io.github.tomboyo.lily.compiler.icg;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Support {

  /** Converts the given name to lowerCamelCase */
  public static String lowerCamelCase(String name) {
    return name.substring(0, 1).toLowerCase() + name.substring(1);
  }

  /** Converts the given name to CapitalCamelCase */
  public static String capitalCamelCase(String name) {
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }

  /**
   * Joins packages together in the given order, returning a normalized package name.
   *
   * <p>For example, joinPackages("", "com.foo", "bar", "") == "com.foo.bar"
   */
  public static String joinPackages(String... packages) {
    return Arrays.stream(packages)
        .filter(x -> !x.isBlank())
        .collect(Collectors.joining("."))
        .toLowerCase();
  }
}
