package io.github.tomboyo.lily.compiler.ast;

/** An operation, such as "createNewBlogPost" corresponding to an OAS operation. */
public record AstOperation(String operationName, AstReference operationClass) implements Ast {}
