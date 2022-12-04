package io.github.tomboyo.lily.compiler.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FqnTest {

  Fqn subject;

  @BeforeEach
  void beforeEach() {
    // the FQN of List<List<String>>
    subject =
        Fqn.newBuilder()
            .packageName("java.util")
            .typeName("List")
            .typeParameters(
                List.of(
                    Fqn.newBuilder()
                        .packageName("java.util")
                        .typeName("List")
                        .typeParameters(
                            List.of(
                                Fqn.newBuilder()
                                    .packageName("java.lang")
                                    .typeName("String")
                                    .build()))
                        .build()))
            .build();
  }

  @Test
  void fullyQualifiedString() {
    assertEquals("java.util.List", subject.toFqString());
  }

  @Test
  void fullyQualifiedParameterizedString() {
    assertEquals("java.util.List<java.util.List<java.lang.String>>", subject.toFqpString());
  }

  @Test
  void asPath() {
    assertEquals(Path.of("java/util/List.java"), subject.toPath());
  }
}
