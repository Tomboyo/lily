package io.github.tomboyo.lily.http.deser;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.nio.ByteBuffer;

/** Deserializes JSON binaries into ByteBuffers */
public class ByteBufferDeserializer extends StdDeserializer<ByteBuffer> {

  public ByteBufferDeserializer() {
    super(ByteBuffer.class);
  }

  @Override
  public ByteBuffer deserialize(JsonParser p, DeserializationContext ctxt)
      throws IOException, JacksonException {
    return ByteBuffer.wrap(p.readValueAs(byte[].class));
  }
}
