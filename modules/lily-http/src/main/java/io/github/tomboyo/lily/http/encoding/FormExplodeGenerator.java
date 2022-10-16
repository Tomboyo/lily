package io.github.tomboyo.lily.http.encoding;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.fasterxml.jackson.core.Base64Variant;
import com.fasterxml.jackson.core.base.GeneratorBase;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLEncoder;

/**
 * Expands all objects according to RFC6570 form-style query expansion or query continuation with
 * the 'explode' modifier, like {@code ?key=value&key=value} and {@code &key=value&key=value}. All
 * key and value pairs are URL encoded, as requried.
 *
 * <p>All objects to be encoded MUST be passed in as a Map with one key-value pair, where the key is
 * the name of the parameter to encode and the value is the object. For example, to encode the
 * parameter named "keys" which is an object {@code { a: 1, b: 2 }}, the encoder should be given the
 * map {@code { keys: { a: 1, b: 2 }}} as an argument. The encoder MAY ignore the parameter name
 * depending on the type of the object to encode.
 *
 * <p>Refer to the following resources:
 *
 * <ul>
 *   <li><a
 *       href="https://github.com/OAI/OpenAPI-Specification/blob/main/versions/3.1.0.md#style-examples">OpenAPI
 *       Style Examples</a>
 *   <li><a href="https://datatracker.ietf.org/doc/html/rfc6570#section-3.2.8">RFC6570 section
 *       3.2.8</a>
 *   <li><a href="https://www.rfc-editor.org/rfc/rfc6570#section-3.2.9">RFC6570 section 3.2.9</a>
 * </ul>
 */
public class FormExplodeGenerator extends GeneratorBase {

  private final Writer writer;

  private final String leadingCharacter;

  // The name of the parameter being expanded, which is given as the key of the singleton key-value
  // pair object passed as argument to this generator. The value is already URL-encoded.
  private String parameterName;

  // True when we still need to write the leading character (e.g. the '?' at the beginning of a
  // query string)
  private boolean writeLeadingCharacter = true;

  // 1 indicates that we have read the parameterName from the wrapping object, and
  // 2 indicates that we are encoding an object argument.
  // 3 or greater suggests a nested object, which is not supported.
  private int objectDepth = 0;

  // The field/key name for the object parameter currently being written.
  private String currentField = null;

  // True when we are writing array elements.
  private boolean isInArray = false;

  public FormExplodeGenerator(String leadingCharacter, Writer writer) {
    super(0, null);
    this.leadingCharacter = leadingCharacter;
    this.writer = writer;
  }

  @Override
  public void writeStartArray() {
    if (isInArray || objectDepth == 2) {
      throw new IllegalStateException("Nested arrays are not supported");
    }

    isInArray = true;
  }

  @Override
  public void writeEndArray() {
    isInArray = false;
  }

  @Override
  public void writeStartObject() {
    objectDepth += 1;
    if (objectDepth > 2 || isInArray) {
      throw new IllegalStateException("Nested objects are not supported");
    }
  }

  @Override
  public void writeEndObject() {
    objectDepth -= 1;
  }

  @Override
  public void writeFieldName(String name) {
    if (objectDepth == 1) {
      if (parameterName != null) {
        throw new IllegalStateException("Expected a top-level singleton map but got a multiton");
      }

      // Retrieve the name of the parameter to encode from the KV wrapper. We don't need to write
      // anything yet -- whether the key is written depends on the type of the object to encode.
      parameterName = URLEncoder.encode(name, UTF_8);
      return;
    }

    currentField = URLEncoder.encode(name, UTF_8);
  }

  @Override
  public void writeString(String text) throws IOException {
    writeJoiner();
    writer.write(URLEncoder.encode(text, UTF_8));
  }

  @Override
  public void writeString(char[] buffer, int offset, int len) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeRawUTF8String(byte[] buffer, int offset, int len) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeUTF8String(byte[] buffer, int offset, int len) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeRaw(String text) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeRaw(String text, int offset, int len) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeRaw(char[] text, int offset, int len) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeRaw(char c) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeBinary(Base64Variant bv, byte[] data, int offset, int len) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeNumber(int v) throws IOException {
    writeJoiner();
    writer.write(Integer.toString(v));
  }

  @Override
  public void writeNumber(long v) throws IOException {
    writeJoiner();
    writer.write(Long.toString(v));
  }

  @Override
  public void writeNumber(BigInteger v) throws IOException {
    writeJoiner();
    writer.write(v.toString());
  }

  @Override
  public void writeNumber(double v) throws IOException {
    writeJoiner();
    writer.write(Double.toString(v));
  }

  @Override
  public void writeNumber(float v) throws IOException {
    writeJoiner();
    writer.write(Float.toString(v));
  }

  @Override
  public void writeNumber(BigDecimal v) throws IOException {
    writeJoiner();
    writer.write(v.toString());
  }

  @Override
  public void writeNumber(String encodedValue) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void writeBoolean(boolean state) throws IOException {
    writeJoiner();
    writer.write(Boolean.toString(state));
  }

  @Override
  public void writeNull() throws IOException {
    writeJoiner();
  }

  @Override
  public void flush() throws IOException {
    writer.flush();
  }

  @Override
  protected void _releaseBuffers() {}

  @Override
  protected void _verifyValueWrite(String typeMsg) {}

  private void writeJoiner() throws IOException {
    if (objectDepth == 2) {
      writeExplodedObjectKey();
    }
    if (isInArray) {
      writeExplodedArrayElementKey();
    } else if (objectDepth == 1) {
      // We are encoding a single primitive value, not an element of an array or object
      writePrimitiveKey();
    }
  }

  /** Write keys for exploded object fields (?field1=value1&field2=value2) */
  private void writeExplodedObjectKey() throws IOException {
    if (writeLeadingCharacter) {
      writer.write(leadingCharacter);
      writeLeadingCharacter = false;
    } else {
      writer.write('&');
    }

    // When writing an object with the 'explode' modifier, we use the field names from the object
    // rather than the given parameterName (like we would instead use for arrays).
    writer.write(currentField);
    writer.write('=');
  }

  /** Write keys for exploded array elements (?paramName=e1&paramName=e2) */
  private void writeExplodedArrayElementKey() throws IOException {
    if (writeLeadingCharacter) {
      writer.write(leadingCharacter);
      writeLeadingCharacter = false;
    } else {
      writer.write('&');
    }

    // When writing an array with the 'explode' modifier, we repeatedly use the given parameterName
    // as keys.
    writer.write(parameterName);
    writer.write('=');
  }

  /** Write the key for the singular primitive value being encoded (?paramName=primitive) */
  private void writePrimitiveKey() throws IOException {
    writer.write(leadingCharacter);
    writer.write(parameterName);
    writer.write('=');
  }
}
