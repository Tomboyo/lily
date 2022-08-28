package io.github.tomboyo.lily.compiler.ast;

import java.util.Set;

/** An operation, such as "createNewBlogPost" corresponding to an OAS operation. */
public record AstOperation(
    String operationName,
    AstReference operationClass,
    String relativePath,
    Set<AstParameter> parameters)
    implements Ast {}
