package com.github.tomboyo.lily.ast;

public class Support {

  public static String toClassCase(String name) {
    return capitalCamelCase(name);
  }

  public static String lowerCamelCase(String name) {
    return name.substring(0, 1).toLowerCase() + name.substring(1);
  }

  /** Converts the given name to CapitalCamelCase */
  public static String capitalCamelCase(String name) {
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }
}
