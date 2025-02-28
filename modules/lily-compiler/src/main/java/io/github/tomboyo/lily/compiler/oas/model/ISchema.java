package io.github.tomboyo.lily.compiler.oas.model;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.DEDUCTION;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = DEDUCTION)
@JsonSubTypes({@Type(None.class), @Type(Ref.class), @Type(Schema.class)})
public sealed interface ISchema permits None, Ref, Schema {}
