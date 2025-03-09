package io.github.tomboyo.lily.compiler.oas.model;

import java.util.Optional;

/* A definition of a Parameter */
public record Parameter(
    Optional<String> name,
    Optional<String> in,
    Optional<String> style,
    Optional<Boolean> explode,
    // IParameter can be a Ref, but a Parameter's schema is never a Ref.
    ISchema schema)
    implements IParameter {}
