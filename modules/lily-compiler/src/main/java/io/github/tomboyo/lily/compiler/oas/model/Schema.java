package io.github.tomboyo.lily.compiler.oas.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record Schema(
    Optional<String> type,
    Optional<String> format,
    Map<String, ISchema> properties,
    ISchema items,
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
    return type.orElse("null").equals("array") || items != None.NONE;
  }
}
