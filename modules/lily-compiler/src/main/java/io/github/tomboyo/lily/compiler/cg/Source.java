package io.github.tomboyo.lily.compiler.cg;

import java.nio.file.Path;

public record Source(Path relativePath, String fqn, String contents) {}
