package io.github.tomboyo.lily.http;

public class UriTemplateException extends RuntimeException {
  public UriTemplateException(String message) {
    super(message);
  }

  public UriTemplateException(String message, Throwable cause) {
    super(message, cause);
  }
}
