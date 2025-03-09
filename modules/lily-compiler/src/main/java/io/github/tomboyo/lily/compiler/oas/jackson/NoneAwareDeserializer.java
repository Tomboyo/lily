package io.github.tomboyo.lily.compiler.oas.jackson;

import static io.github.tomboyo.lily.compiler.oas.model.None.NONE;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;

public class NoneAwareDeserializer extends DelegatingDeserializer {

  public NoneAwareDeserializer(JsonDeserializer<?> d) {
    super(d);
  }

  @Override
  protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
    return new NoneAwareDeserializer(newDelegatee);
  }

  @Override
  public Object getNullValue(DeserializationContext ctxt) throws JsonMappingException {
    return NONE;
  }

  @Override
  public Object getAbsentValue(DeserializationContext ctxt) throws JsonMappingException {
    return NONE;
  }
}
