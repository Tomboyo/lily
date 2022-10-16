package io.github.tomboyo.lily.http.encoding;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.base.GeneratorBase;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Expands all objects according to RFC6570 simple string expansion.
 *
 * <p>See <a
 * href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#style-examples">OpenAPI
 * Style Examples</a> and <a
 * href="https://datatracker.ietf.org/doc/html/rfc6570#section-3.2.2.">RFC6570 section 3.2.2</a>
 */
public class SimpleGenerator extends GeneratorBase {

  private final Writer writer;

  private boolean inObject = false;
  private boolean inArray = false;
  private boolean isFirstArrayElement = true;
  private boolean isFirstObjectField = true;

  public SimpleGenerator(Writer writer) {
    super(0, null);
    this.writer = writer;
  }

  private void writeSeparator() throws IOException {
    if (inArray) {
      if (isFirstArrayElement) {
        isFirstArrayElement = false;
      } else {
        writer.write(",");
      }
    }
  }

  @Override
  public void writeStartArray() throws IOException {
    if (inArray) {
      throw new UnsupportedOperationException("Nested lists are not supported");
    }

    if (inObject) {
      throw new UnsupportedOperationException("Nested lists are not supported");
    }

    inArray = true;
    isFirstArrayElement = true;
  }

  @Override
  public void writeEndArray() throws IOException {
    inArray = false;
  }

  @Override
  public void writeStartObject() throws IOException {
    if (inArray) {
      throw new UnsupportedOperationException("Nested objects are not supported");
    }

    if (inObject) {
      throw new UnsupportedOperationException("Nested objects are not supported");
    }

    inObject = true;
    isFirstObjectField = true;
  }

  @Override
  public void writeEndObject() throws IOException {
    inObject = false;
  }

  @Override
  public void writeFieldName(String name) throws IOException {
    if (isFirstObjectField) {
      isFirstObjectField = false;
    } else {
      writer.write(",");
    }
    writer.write(name);
    writer.write(",");
  }

  @Override
  public void writeString(String text) throws IOException {
    writeSeparator();
    writer.write(text);
  }

  @Override
  public void writeString(char[] buffer, int offset, int len) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeRawUTF8String(byte[] buffer, int offset, int len) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeUTF8String(byte[] buffer, int offset, int len) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeRaw(String text) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeRaw(String text, int offset, int len) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeRaw(char[] text, int offset, int len) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeRaw(char c) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeBinary(Base64Variant bv, byte[] data, int offset, int len) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeNumber(int v) throws IOException {
    writeSeparator();
    writer.write(Integer.toString(v));
  }

  @Override
  public void writeNumber(long v) throws IOException {
    writeSeparator();
    writer.write(Long.toString(v));
  }

  @Override
  public void writeNumber(BigInteger v) throws IOException {
    writeSeparator();
    writer.write(v.toString());
  }

  @Override
  public void writeNumber(double v) throws IOException {
    writeSeparator();
    writer.write(Double.toString(v));
  }

  @Override
  public void writeNumber(float v) throws IOException {
    writeSeparator();
    writer.write(Float.toString(v));
  }

  @Override
  public void writeNumber(BigDecimal v) throws IOException {
    writeSeparator();
    writer.write(v.toString());
  }

  @Override
  public void writeNumber(String encodedValue) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeBoolean(boolean state) throws IOException {
    writeSeparator();
    writer.write(Boolean.toString(state));
  }

  @Override
  public void writeNull() throws IOException {
    writeSeparator();
  }

  @Override
  public void flush() throws IOException {
    writer.flush();
  }

  @Override
  protected void _releaseBuffers() {}

  @Override
  protected void _verifyValueWrite(String typeMsg) throws IOException {}
}
