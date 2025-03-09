package io.github.tomboyo.lily.compiler.oas.model;

import java.util.Map;
import java.util.Optional;

public record Components(Map<String, Optional<ISchema>> schemas) {}
