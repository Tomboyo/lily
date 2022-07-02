package io.github.tomboyo.lily.compiler.icg;

import io.github.tomboyo.lily.compiler.ast.AstReference;

public class StdlibAstReferences {

  public static AstReference astBigInteger() {
    return new AstReference("java.math", "BigInteger");
  }

  public static AstReference astLong() {
    return new AstReference("java.lang", "Long");
  }

  public static AstReference astInteger() {
    return new AstReference("java.lang", "Integer");
  }

  public static AstReference astBigDecimal() {
    return new AstReference("java.math", "BigDecimal");
  }

  public static AstReference astDouble() {
    return new AstReference("java.lang", "Double");
  }

  public static AstReference astFloat() {
    return new AstReference("java.lang", "Float");
  }

  public static AstReference astString() {
    return new AstReference("java.lang", "String");
  }

  public static AstReference astByteArray() {
    return new AstReference("java.lang", "Byte[]");
  }

  public static AstReference astLocalDate() {
    return new AstReference("java.time", "LocalDate");
  }

  public static AstReference astOffsetDateTime() {
    return new AstReference("java.time", "OffsetDateTime");
  }

  public static AstReference astBoolean() {
    return new AstReference("java.lang", "Boolean");
  }

  public static AstReference astListOf(AstReference t) {
    return new AstReference("java.util", "List", t);
  }
}
