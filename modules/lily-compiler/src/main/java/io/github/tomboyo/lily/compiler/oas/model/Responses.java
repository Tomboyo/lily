package io.github.tomboyo.lily.compiler.oas.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.Map;

public record Responses(
    /* Map from https response codes as Strings to their Response objects. Includes the "default" response. */
    @JsonAnySetter Map<String, IResponse> responseMap) {}
