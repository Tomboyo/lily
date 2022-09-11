package io.github.tomboyo.lily.http.deser;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ByteBufferSerializer extends JsonSerializer<ByteBuffer> {
  @Override
  public void serialize(ByteBuffer value, JsonGenerator gen, SerializerProvider serializers)
      throws IOException {
    gen.writeStartArray();
    if (value.hasArray()) {
      for (byte b : value.array()) {
        gen.writeNumber((int) b);
      }
    }
    gen.writeEndArray();
  }
}
