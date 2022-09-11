package io.github.tomboyo.lily.compiler.icg;

import io.github.tomboyo.lily.compiler.ast.AstReference;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import java.util.List;

public class StdlibAstReferences {

  public static AstReference astBigInteger() {
    return new AstReference(Fqn.of("java.math", "BigInteger"), List.of(), true, false);
  }

  public static AstReference astLong() {
    return new AstReference(Fqn.of("java.lang", "Long"), List.of(), true, false);
  }

  public static AstReference astInteger() {
    return new AstReference(Fqn.of("java.lang", "Integer"), List.of(), true, false);
  }

  public static AstReference astBigDecimal() {
    return new AstReference(Fqn.of("java.math", "BigDecimal"), List.of(), true, false);
  }

  public static AstReference astDouble() {
    return new AstReference(Fqn.of("java.lang", "Double"), List.of(), true, false);
  }

  public static AstReference astFloat() {
    return new AstReference(Fqn.of("java.lang", "Float"), List.of(), true, false);
  }

  public static AstReference astString() {
    return new AstReference(Fqn.of("java.lang", "String"), List.of(), true, false);
  }

  public static AstReference astByteArray() {
    return new AstReference(Fqn.of("java.lang", "Byte[]"), List.of(), true, true);
  }

  public static AstReference astLocalDate() {
    return new AstReference(Fqn.of("java.time", "LocalDate"), List.of(), true, false);
  }

  public static AstReference astOffsetDateTime() {
    return new AstReference(Fqn.of("java.time", "OffsetDateTime"), List.of(), true, false);
  }

  public static AstReference astBoolean() {
    return new AstReference(Fqn.of("java.lang", "Boolean"), List.of(), true, false);
  }

  public static AstReference astListOf(AstReference t) {
    return new AstReference(Fqn.of("java.util", "List"), List.of(t), true, false);
  }
}
