package com.github.tomboyo.lily.ast;

public class Support {

  /** Converts the given name to ClassCase. For example, fooBar becomes FooBar. */
  public static String toClassCase(String name) {
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }
}
