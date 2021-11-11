package com.github.tomboyo.lily.ast;

import com.github.tomboyo.lily.ast.type.AstClass;
import com.github.tomboyo.lily.ast.type.AstReference;
import com.github.tomboyo.lily.ast.type.Field;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OasSchemaToAstTest {

  @Test
  public void generateClass() {
    var schema = new Schema()
        .name("MyComponent")
        .type("object")
        .properties(Map.of(
            "foo", new Schema().type("string")
        ));

    var ast = (AstClass) OasSchemaToAst.generateAst(
        "com.foo",
        "MyComponent",
        schema
        ).findAny().orElseThrow();

    assertEquals(new AstClass("MyComponent", List.of(new Field(
        new AstReference("java.lang", "String"),
        "foo"
    ))), ast);
  }
}
