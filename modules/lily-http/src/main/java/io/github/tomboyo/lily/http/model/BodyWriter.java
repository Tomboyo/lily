package io.github.tomboyo.lily.http.model;

@FunctionalInterface
public interface BodyWriter<BodyParameters> {
  byte[] writeBody(BodyParameters parameters) throws Exception;
}
