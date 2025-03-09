package io.github.tomboyo.lily.compiler.oas.model;

import java.util.Map;

public record RequestBody(Map<String, MediaType> content) implements IRequestBody {}
