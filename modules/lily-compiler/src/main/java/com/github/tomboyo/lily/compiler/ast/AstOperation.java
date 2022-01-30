package com.github.tomboyo.lily.compiler.ast;

import java.util.Set;

public record AstOperation(Set<String> tags, String id, Method method, String path) {
  public enum Method {
    DELETE,
    GET,
    HEAD,
    OPTIONS,
    PATCH,
    POST,
    PUT,
    TRACE
  }
}
