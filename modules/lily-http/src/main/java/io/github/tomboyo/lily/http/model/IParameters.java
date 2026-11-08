package io.github.tomboyo.lily.http.model;

import java.util.Map;

/** Key-value pairs for path, query, header, and cookie parameters. */
public interface IParameters {
  static NoParameters none() {
    return new NoParameters();
  }

  Map<String, Object> asMap();
}
