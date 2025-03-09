package io.github.tomboyo.lily.compiler.oas.model;

import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record Schema(
    Optional<String> type,
    Optional<String> format,
    Map<String, Optional<ISchema>> properties,
    Optional<ISchema> items,
    List<ISchema> allOf,
    List<ISchema> anyOf,
    List<ISchema> oneOf,
    List<ISchema> not,
    List<String> required,
    Optional<Boolean> nullable)
    implements ISchema {
  public boolean isComposed() {
    return !allOf.isEmpty() || !anyOf.isEmpty() || !oneOf.isEmpty() || !not.isEmpty();
  }

  public boolean isObject() {
    return type.orElse("null").equals("object") || !properties.isEmpty() || isComposed();
  }

  public boolean isArray() {
    return type.orElse("null").equals("array") || items.isPresent();
  }

  /**
   * Like properties(), but returns only the well-formed KV pairs (i.e. those with non-empty
   * values).
   */
  public Map<String, ISchema> getProperties() {
    return properties.entrySet().stream()
        .filter(entry -> entry.getValue().isPresent())
        .collect(toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
  }
}
