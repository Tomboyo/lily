package io.github.tomboyo.lily.compiler.oas.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.HashMap;
import java.util.Optional;

/** Wraps a HashMap with a null-safe API. */
public class OMap<K, V> {
  private static final OMap<?, ?> EMPTY = new OMap<>(new HashMap<>());

  private final HashMap<K, V> delegate;

  @SuppressWarnings("unchecked")
  @JsonCreator
  public static <T, U> OMap<T, U> of() {
    return (OMap<T, U>) EMPTY;
  }

  @JsonCreator
  public OMap(HashMap<K, V> delegate) {
    this.delegate = delegate;
  }

  public Optional<V> get(K key) {
    return Optional.ofNullable(delegate.get(key));
  }
}
