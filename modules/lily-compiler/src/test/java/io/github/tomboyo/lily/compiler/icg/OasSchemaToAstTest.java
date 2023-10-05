package io.github.tomboyo.lily.compiler.icg;

// TODO
class OasSchemaToAstTest {
  //
  //  /** A list of scalar types and formats, and the java types they evaluate to. * */
  //  public static Stream<Arguments> scalarsSource() {
  //    return Stream.of(
  //        arguments("boolean", null, astBoolean()),
  //        arguments("boolean", "unsupported-format", astBoolean()),
  //        arguments("integer", null, astBigInteger()),
  //        arguments("integer", "unsupported-format", astBigInteger()),
  //        arguments("integer", "int32", astInteger()),
  //        arguments("integer", "int64", astLong()),
  //        arguments("number", null, astBigDecimal()),
  //        arguments("number", "unsupported-format", astBigDecimal()),
  //        arguments("number", "double", astDouble()),
  //        arguments("number", "float", astFloat()),
  //        arguments("string", null, astString()),
  //        arguments("string", "unsupportedFormat", astString()),
  //        arguments("string", "password", astString()),
  //        arguments("string", "byte", astByteBuffer()),
  //        arguments("string", "binary", astByteBuffer()),
  //        arguments("string", "date", astLocalDate()),
  //        arguments("string", "date-time", astOffsetDateTime()));
  //  }
  //
  //  @Test
  //  public void unsupportedSchemaTypes() {
  //    assertThrows(
  //        IllegalArgumentException.class,
  //        () ->
  //            OasSchemaToAst.evaluate(
  //                PackageName.of("p"),
  //                SimpleName.of("MyBadSchema"),
  //                new Schema<>().type("unsupported-type")),
  //        "Unsupported types trigger runtime exceptions.");
  //  }
  //
  //  @Nested
  //  class ScalarSchemas {
  //    @ParameterizedTest
  //    @MethodSource("io.github.tomboyo.lily.compiler.icg.OasSchemaToAstTest#scalarsSource")
  //    void evaluate(String oasType, String oasFormat, Fqn expectedRef) {
  //      var actual =
  //          OasSchemaToAst.evaluate(
  //              PackageName.of("p"),
  //              SimpleName.of("fieldName"),
  //              new Schema().type(oasType).format(oasFormat));
  //
  //      assertEquals(
  //          new Pair<>(expectedRef, Set.of()),
  //          actual.mapRight(stream -> stream.collect(toSet())),
  //          "returns a standard type reference and no AST since nothing was generated");
  //    }
  //  }
  //
  //  @Nested
  //  class ObjectSchemas {
  //    @ParameterizedTest
  //    @MethodSource("io.github.tomboyo.lily.compiler.icg.OasSchemaToAstTest#scalarsSource")
  //    void evaluateWithScalarProperty(String oasType, String oasFormat, Fqn expectedRef) {
  //      var actual =
  //          OasSchemaToAst.evaluate(
  //              PackageName.of("p"),
  //              SimpleName.of("MyObject"),
  //              new ObjectSchema()
  //                  .properties(Map.of("myField", new Schema().type(oasType).format(oasFormat))));
  //
  //      assertEquals(
  //          new Pair<>(
  //              Fqn.newBuilder().packageName("p").typeName("MyObject").build(),
  //              Set.of(
  //                  AstClass.of(
  //                      Fqn.newBuilder().packageName("p").typeName("MyObject").build(),
  //                      List.of(new Field(expectedRef, SimpleName.of("myField"), "myField"))))),
  //          actual.mapRight(stream -> stream.collect(toSet())),
  //          "returns an AstReference for the generated type and its AST");
  //    }
  //
  //    @Test
  //    void evaluateWithInlineObjectProperty() {
  //      var actual =
  //          OasSchemaToAst.evaluate(
  //              PackageName.of("p"),
  //              SimpleName.of("MyObject"),
  //              new ObjectSchema()
  //                  .properties(
  //                      Map.of(
  //                          "myInnerObject",
  //                          new ObjectSchema()
  //                              .properties(Map.of("myField", new Schema().type("boolean"))))));
  //
  //      assertEquals(
  //          new Pair<>(
  //              Fqn.newBuilder().packageName("p").typeName("MyObject").build(),
  //              Set.of(
  //                  AstClass.of(
  //                      Fqn.newBuilder().packageName("p").typeName("MyObject").build(),
  //                      List.of(
  //                          new Field(
  //                              Fqn.newBuilder()
  //                                  .packageName("p.myobject")
  //                                  .typeName("MyInnerObject")
  //                                  .build(),
  //                              SimpleName.of("myInnerObject"),
  //                              "myInnerObject"))),
  //                  AstClass.of(
  //
  // Fqn.newBuilder().packageName("p.myobject").typeName("MyInnerObject").build(),
  //                      List.of(new Field(astBoolean(), SimpleName.of("myField"), "myField"))))),
  //          actual.mapRight(stream -> stream.collect(toSet())),
  //          "returns an AstReference for the outer generated type but the AST for both the outer
  // and"
  //              + " nested types");
  //    }
  //
  //    @Test
  //    void evaluateWithReferenceProperty() {
  //      var actual =
  //          OasSchemaToAst.evaluate(
  //              PackageName.of("p"),
  //              SimpleName.of("MyObject"),
  //              new ObjectSchema()
  //                  .properties(Map.of("myField", new
  // Schema().$ref("#/components/schemas/MyRef"))));
  //
  //      assertEquals(
  //          new Pair<>(
  //              Fqn.newBuilder().packageName("p").typeName("MyObject").build(),
  //              Set.of(
  //                  AstClass.of(
  //                      Fqn.newBuilder().packageName("p").typeName("MyObject").build(),
  //                      List.of(
  //                          new Field(
  //                              Fqn.newBuilder().packageName("p").typeName("MyRef").build(),
  //                              SimpleName.of("myField"),
  //                              "myField"))))),
  //          actual.mapRight(stream -> stream.collect(toSet())),
  //          "returns an AstReference for the outer generated type and its AST, but no AST for the"
  //              + " referenced type, which must be evaluated separately");
  //    }
  //
  //    @Test
  //    void evaluateWithInlineArrayProperty() {
  //      var actual =
  //          OasSchemaToAst.evaluate(
  //              PackageName.of("p"),
  //              SimpleName.of("MyObject"),
  //              new ObjectSchema()
  //                  .properties(
  //                      Map.of("myField", new ArraySchema().items(new
  // Schema().type("boolean")))));
  //
  //      assertEquals(
  //          new Pair<>(
  //              Fqn.newBuilder().packageName("p").typeName("MyObject").build(),
  //              Set.of(
  //                  AstClass.of(
  //                      Fqn.newBuilder().packageName("p").typeName("MyObject").build(),
  //                      List.of(
  //                          new Field(
  //                              astListOf(astBoolean()), SimpleName.of("myField"), "myField"))))),
  //          actual.mapRight(stream -> stream.collect(toSet())),
  //          "returns an AstReference to the generated type and its AST, which does not generate
  // any"
  //              + " new type for the nested array");
  //    }
  //
  //    @Test
  //    void evaluateWithMultipleProperties() {
  //      var actual =
  //          OasSchemaToAst.evaluate(
  //              PackageName.of("p"),
  //              SimpleName.of("MyObject"),
  //              new ObjectSchema()
  //                  .properties(
  //                      Map.of(
  //                          "myField1", new Schema().type("boolean"),
  //                          "myField2", new Schema().$ref("#/components/schemas/MyRef"))));
  //
  //      assertEquals(
  //          new Pair<>(
  //              Fqn.newBuilder().packageName("p").typeName("MyObject").build(),
  //              Set.of(
  //                  AstClass.of(
  //                      Fqn.newBuilder().packageName("p").typeName("MyObject").build(),
  //                      List.of(
  //                          new Field(astBoolean(), SimpleName.of("myField1"), "myField1"),
  //                          new Field(
  //                              Fqn.newBuilder().packageName("p").typeName("MyRef").build(),
  //                              SimpleName.of("myField2"),
  //                              "myField2"))))),
  //          actual.mapRight(stream -> stream.collect(toSet())),
  //          "The AST contains one field for each of multiple properties");
  //    }
  //  }
  //
  //  @Nested
  //  class ArraySchemas {
  //    @ParameterizedTest
  //    @MethodSource("io.github.tomboyo.lily.compiler.icg.OasSchemaToAstTest#scalarsSource")
  //    void evaluateWithScalarItem(String oasType, String oasFormat, Fqn expectedRef) {
  //      var actual =
  //          OasSchemaToAst.evaluate(
  //              PackageName.of("p"),
  //              SimpleName.of("MyArray"),
  //              new ArraySchema().items(new Schema().type(oasType).format(oasFormat)));
  //
  //      assertEquals(
  //          new Pair<>(astListOf(expectedRef), Set.of()),
  //          actual.mapRight(stream -> stream.collect(toSet())),
  //          "returns an AstReference for the list, but no AST since no new types are generated");
  //    }
  //
  //    @Test
  //    void evaluateWithInlineObjectItem() {
  //      var actual =
  //          OasSchemaToAst.evaluate(
  //              PackageName.of("p"),
  //              SimpleName.of("MyArray"),
  //              new ArraySchema()
  //                  .items(
  //                      new ObjectSchema()
  //                          .properties(Map.of("myField", new Schema().type("boolean")))));
  //
  //      assertEquals(
  //          new Pair<>(
  //              astListOf(Fqn.newBuilder().packageName("p").typeName("MyArrayItem").build()),
  //              Set.of(
  //                  AstClass.of(
  //                      Fqn.newBuilder().packageName("p").typeName("MyArrayItem").build(),
  //                      List.of(new Field(astBoolean(), SimpleName.of("myField"), "myField"))))),
  //          actual.mapRight(stream -> stream.collect(toSet())),
  //          "defines the inline object in the current package with the -Item suffix in its class"
  //              + " name");
  //    }
  //
  //    @Test
  //    void evaluateWithReferenceItem() {
  //      var actual =
  //          OasSchemaToAst.evaluate(
  //              PackageName.of("p"),
  //              SimpleName.of("MyArray"),
  //              new ArraySchema().items(new Schema<>().$ref("#/components/schemas/MyRef")));
  //
  //      assertEquals(
  //          new Pair<>(
  //              astListOf(Fqn.newBuilder().packageName("p").typeName("MyRef").build()), Set.of()),
  //          actual.mapRight(stream -> stream.collect(toSet())),
  //          "returns an AstReference for the list, but no AST since no new types are generated
  // (we"
  //              + " assume the reference is evaluated separately)");
  //    }
  //
  //    @Test
  //    void evaluateWithArrayItem() {
  //      var actual =
  //          OasSchemaToAst.evaluate(
  //              PackageName.of("p"),
  //              SimpleName.of("MyArray"),
  //              new ArraySchema().items(new ArraySchema().items(new Schema<>().type("boolean"))));
  //
  //      assertEquals(
  //          new Pair<>(astListOf(astListOf(astBoolean())), Set.of()),
  //          actual.mapRight(stream -> stream.collect(toSet())),
  //          "returns a compose list AstReference and no AST since no new types are generated");
  //    }
  //  }
}
