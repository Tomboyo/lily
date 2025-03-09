package io.github.tomboyo.lily.compiler.oas.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record PathItem(
    @JsonSetter(nulls = Nulls.AS_EMPTY) List<IParameter> parameters,
    Optional<Operation> get,
    Optional<Operation> put,
    Optional<Operation> post,
    Optional<Operation> delete,
    Optional<Operation> options,
    Optional<Operation> head,
    Optional<Operation> patch,
    Optional<Operation> trace) {
  public Map<String, Operation> operationsMap() {
    var map = new HashMap<String, Operation>();
    get.ifPresent(x -> map.put("GET", x));
    put.ifPresent(x -> map.put("PUT", x));
    post.ifPresent(x -> map.put("POST", x));
    delete.ifPresent(x -> map.put("DELETE", x));
    options.ifPresent(x -> map.put("OPTIONS", x));
    head.ifPresent(x -> map.put("HEAD", x));
    patch.ifPresent(x -> map.put("PATCH", x));
    trace.ifPresent(x -> map.put("TRACE", x));
    return map;
  }
}
