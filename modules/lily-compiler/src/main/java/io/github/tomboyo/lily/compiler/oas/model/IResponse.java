package io.github.tomboyo.lily.compiler.oas.model;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.DEDUCTION;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = DEDUCTION)
@JsonSubTypes({
  @JsonSubTypes.Type(None.class),
  @JsonSubTypes.Type(Ref.class),
  @JsonSubTypes.Type(Response.class)
})
public sealed interface IResponse permits None, Ref, Response {}
