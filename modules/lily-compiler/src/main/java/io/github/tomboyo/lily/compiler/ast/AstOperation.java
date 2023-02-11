package io.github.tomboyo.lily.compiler.ast;

import java.util.List;
import java.util.Optional;

/**
 * An operation, such as "createNewBlogPost" corresponding to an OAS operation.
 *
 * <p>The parameters field list reflects the order of parameters as specified in the OAS.
 */
public record AstOperation(
    SimpleName operationName,
    Fqn name,
    String method,
    String relativePath,
    List<OperationParameter> parameters,
    Optional<Fqn> requestBody,
    Fqn responseName)
    implements Ast {}
