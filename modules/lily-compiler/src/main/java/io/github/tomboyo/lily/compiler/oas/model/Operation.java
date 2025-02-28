package io.github.tomboyo.lily.compiler.oas.model;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public record Operation(
    //    @JsonSetter(nulls = Nulls.AS_EMPTY)
    Set<String> tags,
    Optional<String> operationId,
    //    @JsonSetter(nulls = Nulls.AS_EMPTY)
    List<IParameter> parameters,
    Optional<Responses> responses,
    Optional<IRequestBody> requestBody) {}
