package io.github.tomboyo.lily.compiler.icg;

import io.github.tomboyo.lily.compiler.ast.Fqn;
import java.util.List;

public class StdlibFqns {

  public static Fqn astBigInteger() {
    return Fqn.newBuilder().packageName("java.math").typeName("BigInteger").build();
  }

  public static Fqn astLong() {
    return Fqn.newBuilder().typeName("Long").build();
  }

  public static Fqn astInteger() {
    return Fqn.newBuilder().typeName("Integer").build();
  }

  public static Fqn astBigDecimal() {
    return Fqn.newBuilder().packageName("java.math").typeName("BigDecimal").build();
  }

  public static Fqn astDouble() {
    return Fqn.newBuilder().typeName("Double").build();
  }

  public static Fqn astFloat() {
    return Fqn.newBuilder().typeName("Float").build();
  }

  public static Fqn astString() {
    return Fqn.newBuilder().typeName("String").build();
  }

  public static Fqn astByteBuffer() {
    return Fqn.newBuilder().packageName("java.nio").typeName("ByteBuffer").build();
  }

  public static Fqn astLocalDate() {
    return Fqn.newBuilder().packageName("java.time").typeName("LocalDate").build();
  }

  public static Fqn astOffsetDateTime() {
    return Fqn.newBuilder().packageName("java.time").typeName("OffsetDateTime").build();
  }

  public static Fqn astBoolean() {
    return Fqn.newBuilder().typeName("Boolean").build();
  }

  public static Fqn astListOf(Fqn t) {
    return Fqn.newBuilder()
        .packageName("java.util")
        .typeName("List")
        .typeParameters(List.of(t))
        .build();
  }
}
