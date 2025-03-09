package io.github.tomboyo.lily.compiler.oas.model;

import java.util.Map;
import java.util.Optional;

public record OpenApi(
    /* OpenAPI version string */
    Optional<String> openapi, Optional<Components> components, Map<String, PathItem> paths) {}
