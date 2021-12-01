package com.github.tomboyo.lily.render;

import com.github.tomboyo.lily.ast.type.AstClass;
import com.github.tomboyo.lily.ast.type.AstField;
import com.github.tomboyo.lily.ast.type.AstReference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AstToJavaTest {
  @Test
  public void renderAstClass() {
    assertEquals(
        """
            package com.foo;
            public class MyClass {
            private java.lang.String myString;
            public java.lang.String myString() { return myString; }
            public MyClass myString(java.lang.String myString) { this.myString = myString; return this; }
            }""",
        AstToJava.renderAst("com.foo", new AstClass("MyClass", List.of(
            new AstField(new AstReference("java.lang", "String"), "myString")
        )))
    );
  }
}
