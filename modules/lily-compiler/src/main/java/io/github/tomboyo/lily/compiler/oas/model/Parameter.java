package io.github.tomboyo.lily.compiler.oas.model;

import java.util.Optional;

/* A definition of a Parameter */
public record Parameter(
    Optional<String> name,
    Optional<String> in,
    Optional<String> style,
    Optional<Boolean> explode,
    Optional<ISchema> schema)
    implements IParameter {}
