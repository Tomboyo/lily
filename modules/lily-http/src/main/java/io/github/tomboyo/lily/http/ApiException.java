package io.github.tomboyo.lily.http;

/**
 * Thrown due to an unrecoverable, unexpected exception while preparing or sending an HTTP request.
 */
public final class ApiException extends RuntimeException {
  public ApiException(String message) {
    super(message);
  }

  public ApiException(String message, Exception cause) {
    super(message, cause);
  }
}
