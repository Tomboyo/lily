package io.github.tomboyo.lily.http.model;

import java.util.Map;

/**
 * Used when there are no parameters in a location of an operation, e.g. no body parameters or no
 * path parameters.
 */
public class NoParameters implements IParameters {
  @Override
  public Map<String, Object> asMap() {
    return Map.of();
  }
}
