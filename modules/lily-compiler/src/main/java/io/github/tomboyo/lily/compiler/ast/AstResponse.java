package io.github.tomboyo.lily.compiler.ast;

import java.util.Optional;

public record AstResponse(
    Fqn name, Optional<Fqn> headersName, Optional<Fqn> contentName, Fqn sumTypeName)
    implements Definition {}
