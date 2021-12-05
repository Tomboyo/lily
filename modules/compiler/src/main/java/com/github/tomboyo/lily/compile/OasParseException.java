package com.github.tomboyo.lily.compile;

public class OasParseException extends Exception {
  public OasParseException(String message, Throwable cause) {
    super(message, cause);
  }

  public OasParseException(String message) {
    super(message);
  }
}
