package com.github.tomboyo.lily.http.encoding;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.base.GeneratorBase;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;

// explode? empty string array            object
// y        n/a   blue   blue,black,brown R=100,G=200,B=150
// n        n/a   blue   blue,black,brown R,100,G,200,B,150
public class SimpleGenerator extends GeneratorBase {

  private final Writer writer;

  private boolean inObject = false;
  private boolean tailField = false;

  public SimpleGenerator(Writer writer) {
    super(0, (ObjectCodec) null);
    this.writer = writer;
  }

  @Override
  public void writeStartArray() throws IOException {}

  @Override
  public void writeEndArray() throws IOException {}

  @Override
  public void writeStartObject() throws IOException {
    inObject = true;
  }

  @Override
  public void writeEndObject() throws IOException {
    inObject = false;
  }

  @Override
  public void writeFieldName(String name) throws IOException {
    if (tailField) {
      writer.write(",");
    }
    writer.write(name);
    writer.write(",");
    tailField = true;
  }

  @Override
  public void writeString(String text) throws IOException {
    writer.write(text);
  }

  @Override
  public void writeString(char[] buffer, int offset, int len) throws IOException {
    writer.write(buffer, offset, len);
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
    writer.write(Integer.toString(v));
  }

  @Override
  public void writeNumber(long v) throws IOException {
    writer.write(Long.toString(v));
  }

  @Override
  public void writeNumber(BigInteger v) throws IOException {
    writer.write(v.toString());
  }

  @Override
  public void writeNumber(double v) throws IOException {
    writer.write(Double.toString(v));
  }

  @Override
  public void writeNumber(float v) throws IOException {
    writer.write(Float.toString(v));
  }

  @Override
  public void writeNumber(BigDecimal v) throws IOException {
    writer.write(v.toString());
  }

  @Override
  public void writeNumber(String encodedValue) throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeBoolean(boolean state) throws IOException {}

  @Override
  public void writeNull() throws IOException {}

  @Override
  public void flush() throws IOException {
    writer.flush();
  }

  @Override
  protected void _releaseBuffers() {}

  @Override
  protected void _verifyValueWrite(String typeMsg) throws IOException {}
}
