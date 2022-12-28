package io.github.tomboyo.lily.compiler.ast;

/**
 * A parameter of an API operation, like a path parameter or a query string parameter.
 *
 * @param name The Lily name for this parameter which appears in the generated API
 * @param apiName The OAS name for this parameter which appears in URL query strings and JSON
 * @param location Where the parameter goes
 * @param encoding How to encode the parameter into an HTTP request
 * @param typeName The FQN of the parameter's type
 */
public record OperationParameter(
    SimpleName name,
    String apiName,
    ParameterLocation location,
    ParameterEncoding encoding,
    Fqn typeName) {}
