package io.github.tomboyo.lily.http.model;

public record Operation<Body, Parameters extends ParameterBindings<?, ?, ?, ?, Body>, Response>(
    String httpMethod,
    String pathTemplate,
    String queryTemplate,
    Parameters parameters,
    BodyWriter<Body> bodyWriter,
    ResponseReader<Response> responseReader) {}
