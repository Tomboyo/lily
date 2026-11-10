package io.github.tomboyo.lily.http.model;

@FunctionalInterface
public interface BodyWriter<BodyParameters> {

  /**
   * A BodyWriter suitable to use when there is no body associated with an
   * {@link Operation}.
   */
  static <Any> BodyWriter<Any> none() {
    return _input -> null;
  }

  byte[] writeBody(BodyParameters parameters) throws Exception;
}
