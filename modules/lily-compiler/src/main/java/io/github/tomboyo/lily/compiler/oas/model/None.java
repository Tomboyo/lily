package io.github.tomboyo.lily.compiler.oas.model;

/**
 * Represents the value of a field that was omitted or set to null. This could be because the OAS
 * defines a default value for the missing value, or because the OpenAPI specification is malformed.
 */
public final class None implements IExplode, IRequestBody, IResponse, IParameter, IHeader {
  public static final None NONE = new None();

  private None() {}
}
