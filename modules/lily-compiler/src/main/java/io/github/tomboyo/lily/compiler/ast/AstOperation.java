package io.github.tomboyo.lily.compiler.ast;

import java.util.List;

/**
 * An operation, such as "createNewBlogPost" corresponding to an OAS operation.
 *
 * <p>The parameters field list reflects the order of parameters as specified in the OAS.
 */
public record AstOperation(
    SimpleName operationName,
    AstReference operationClass,
    String method,
    String relativePath,
    List<AstParameter> parameters)
    implements Ast {}
