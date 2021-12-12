package com.github.tomboyo.lily.render;

import com.github.tomboyo.lily.ast.type.AstClass;
import com.github.tomboyo.lily.ast.type.AstClassAlias;
import com.github.tomboyo.lily.ast.type.AstField;
import com.github.tomboyo.lily.ast.type.AstReference;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AstToJavaTest {
  @Test
  public void renderAstClass() {
    // Renders package declaration and new class declaration.
    // Renders fields, getters and setters.
    // Renders type parameters for generic types.
    assertEquals(
        new Source(
            Path.of("com/foo/MyClass.java"),
            """
                package com.foo;
                public record MyClass(
                    java.lang.String myString,
                    java.util.List<com.foo.myclass.MyListItem> myList
                ) {}"""
        ),
        AstToJava.renderAst(
            new AstClass(
                "com.foo",
                "MyClass",
                List.of(
                    new AstField(
                        new AstReference(
                            "java.lang",
                            "String"),
                        "myString"),
                    new AstField(
                        new AstReference(
                            "java.util",
                            "List",
                            List.of(
                                new AstReference(
                                    "com.foo.myclass",
                                    "MyListItem"))),
                        "myList")))));
  }

  @Test
  public void renderAstClassAlias() {
    assertEquals(
        new Source(
            Path.of("com/foo/MyAlias.java"),
            """
                package com.foo;
                public record MyAlias(
                    java.lang.String value
                ) {
                  @com.fasterxml.jackson.annotation.JsonCreator
                  public static MyAlias creator(java.lang.String value) { return new MyAlias(value); }
                  @com.fasterxml.jackson.annotation.JsonValue
                  public java.lang.String value() { return value; }
                }"""
        ),
        AstToJava.renderAst(
            new AstClassAlias("com.foo", "MyAlias",
                new AstReference("java.lang", "String")))
    );
  }
}
