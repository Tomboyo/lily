package io.github.tomboyo.lily.compiler.oas.model;

import java.util.Optional;

public record Header(Optional<ISchema> schema) implements IHeader {}
