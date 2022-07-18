package io.github.tomboyo.lily.compiler.icg;

import io.github.tomboyo.lily.compiler.ast.AstReference;
import java.util.List;

public class StdlibAstReferences {

  public static AstReference astBigInteger() {
    return new AstReference("java.math", "BigInteger", List.of(), true);
  }

  public static AstReference astLong() {
    return new AstReference("java.lang", "Long", List.of(), true);
  }

  public static AstReference astInteger() {
    return new AstReference("java.lang", "Integer", List.of(), true);
  }

  public static AstReference astBigDecimal() {
    return new AstReference("java.math", "BigDecimal", List.of(), true);
  }

  public static AstReference astDouble() {
    return new AstReference("java.lang", "Double", List.of(), true);
  }

  public static AstReference astFloat() {
    return new AstReference("java.lang", "Float", List.of(), true);
  }

  public static AstReference astString() {
    return new AstReference("java.lang", "String", List.of(), true);
  }

  public static AstReference astByteArray() {
    return new AstReference("java.lang", "Byte[]", List.of(), true);
  }

  public static AstReference astLocalDate() {
    return new AstReference("java.time", "LocalDate", List.of(), true);
  }

  public static AstReference astOffsetDateTime() {
    return new AstReference("java.time", "OffsetDateTime", List.of(), true);
  }

  public static AstReference astBoolean() {
    return new AstReference("java.lang", "Boolean", List.of(), true);
  }

  public static AstReference astListOf(AstReference t) {
    return new AstReference("java.util", "List", List.of(t), true);
  }
}
