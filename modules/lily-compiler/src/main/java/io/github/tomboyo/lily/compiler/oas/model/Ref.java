package io.github.tomboyo.lily.compiler.oas.model;

public record Ref(String $ref) implements IRequestBody, IParameter, IResponse, IHeader, ISchema {}
