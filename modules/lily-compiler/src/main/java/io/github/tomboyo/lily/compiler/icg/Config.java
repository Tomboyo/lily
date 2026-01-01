package io.github.tomboyo.lily.compiler.icg;

public class Config {
  /**
   * @return whether the compiler is being run in dev mode (when in-development features should be
   *     enabled.)
   */
  public static boolean isDevMode() {
    return "dev".equalsIgnoreCase(System.getProperty("io.github.tomboyo.lily.mode", "production"));
  }
}
