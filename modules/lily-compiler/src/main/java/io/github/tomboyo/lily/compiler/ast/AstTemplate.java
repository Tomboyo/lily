package io.github.tomboyo.lily.compiler.ast;

import java.util.List;

public record AstTemplate(
    Fqn name,
    List<OperationParameter> pathParameters) implements Ast {}