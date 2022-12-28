package io.github.tomboyo.lily.compiler.ast;

/**
 * A path parameter for an operation
 *
 * @param name The Lily name for this parameter which appears in the generated API
 * @param apiName The OAS name for this parameter which appears in URL query strings and JSON
 */
public record OperationParameter(
    SimpleName name,
    String apiName,
    ParameterLocation location,
    ParameterEncoding encoding,
    Fqn typeName) {}
