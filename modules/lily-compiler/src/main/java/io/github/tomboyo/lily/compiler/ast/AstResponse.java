package io.github.tomboyo.lily.compiler.ast;

public record AstResponse(Fqn name, Fqn headersName, Fqn contentName, Fqn sumTypeName)
    implements Ast {}
