package com.github.tomboyo.lily.http.encoding;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.base.GeneratorBase;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Expands all objects according to RFC6570 form string expansion.
 *
 * <p>See https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#style-examples
 * and https://datatracker.ietf.org/doc/html/rfc6570#section-3.2.8.
 */
public class FormExplodeGenerator extends GeneratorBase {

  private final Writer writer;

  private boolean isObjectStarted = false;
  private String lastFieldName = null;
  private boolean isInArray = false;
  private boolean isFirstArrayItem = true;

  public FormExplodeGenerator(Writer writer) {
    super(0, null);
    this.writer = writer;
  }

  private void handleExplodedArray() throws IOException {
    if (isInArray) {
      // We need to write the lst field name before writing the value: &key=
      if (!isFirstArrayItem) {
        writer.write('&');
        writer.write(lastFieldName);
        writer.write('=');
      } else {
        // We just wrote &key= or ?key= for the field name, so we can write the value next.
        isFirstArrayItem = false;
      }
    }
  }

  @Override
  public void writeStartArray() throws IOException {
    isInArray = true;
    isFirstArrayItem = true;
  }

  @Override
  public void writeEndArray() throws IOException {
    isInArray = false;
  }

  @Override
  public void writeStartObject() throws IOException {}

  @Override
  public void writeEndObject() throws IOException {}

  @Override
  public void writeFieldName(String name) throws IOException {
    if (isObjectStarted) {
      writer.write('&');
    } else {
      writer.write('?');
      isObjectStarted = true;
    }

    lastFieldName = name;
    writer.write(name);
    writer.write('=');
  }

  @Override
  public void writeString(String text) throws IOException {
    handleExplodedArray();
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
    handleExplodedArray();
    writer.write(Integer.toString(v));
  }

  @Override
  public void writeNumber(long v) throws IOException {
    handleExplodedArray();
    writer.write(Long.toString(v));
  }

  @Override
  public void writeNumber(BigInteger v) throws IOException {
    handleExplodedArray();
    writer.write(v.toString());
  }

  @Override
  public void writeNumber(double v) throws IOException {
    handleExplodedArray();
    writer.write(Double.toString(v));
  }

  @Override
  public void writeNumber(float v) throws IOException {
    handleExplodedArray();
    writer.write(Float.toString(v));
  }

  @Override
  public void writeNumber(BigDecimal v) throws IOException {
    handleExplodedArray();
    writer.write(v.toString());
  }

  @Override
  public void writeNumber(String encodedValue) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeBoolean(boolean state) throws IOException {
    handleExplodedArray();
    writer.write(Boolean.toString(state));
  }

  @Override
  public void writeNull() throws IOException {
    throw new UnsupportedOperationException();
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
