package io.github.tomboyo.lily.compiler.oas.jackson;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import io.github.tomboyo.lily.compiler.oas.model.*;
import java.util.Arrays;

public class NoneAwareDeserializerModifier extends BeanDeserializerModifier {
  @Override
  public JsonDeserializer<?> modifyDeserializer(
      DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> deserializer) {
    var permitted = beanDesc.getBeanClass().getPermittedSubclasses();
    if (permitted != null && Arrays.asList(permitted).contains(None.class)) {
      return new NoneAwareDeserializer(deserializer);
    }
    return super.modifyDeserializer(config, beanDesc, deserializer);
  }
}
