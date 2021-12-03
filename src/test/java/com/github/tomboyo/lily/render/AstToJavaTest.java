package com.github.tomboyo.lily.render;

import com.github.tomboyo.lily.ast.type.AstClass;
import com.github.tomboyo.lily.ast.type.AstField;
import com.github.tomboyo.lily.ast.type.AstReference;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AstToJavaTest {
  @Test
  public void renderAstClass() {
    assertEquals(
        new Source(
            Path.of("com/foo/MyClass.java"),
            """
            package com.foo;
            public class MyClass {
            private java.lang.String myString;
            public java.lang.String myString() { return myString; }
            public MyClass myString(java.lang.String myString) { this.myString = myString; return this; }
            }"""
        ),
        AstToJava.renderAst(new AstClass("com.foo","MyClass", List.of(
            new AstField(new AstReference("java.lang", "String"), "myString")
        )))
    );
  }
}
