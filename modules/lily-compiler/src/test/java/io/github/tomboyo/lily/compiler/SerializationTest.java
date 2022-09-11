package io.github.tomboyo.lily.compiler;

import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;
import static io.github.tomboyo.lily.compiler.CompilerSupport.compileOas;
import static io.github.tomboyo.lily.compiler.CompilerSupport.deleteGeneratedSources;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests that all generated sources serialize and deserialize to expected values. */
public class SerializationTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static String packageName;

  static {
    MAPPER
        .registerModule(new JavaTimeModule())
        // Prefer ISO-like formats instead of arrays and floats
        .configure(WRITE_DATES_AS_TIMESTAMPS, false);
  }

  @BeforeAll
  public static void beforeAll() throws OasParseException {
    packageName =
        compileOas(
            """
            openapi: 3.0.2
            info:
              title: MultipleTags
              description: "An operation with multiple tags"
              version: 0.1.0
            components:
              schemas:
                # An object with kebab, snake, camel, and pascal case parameters
                ParameterCaseObject:
                  type: object
                  properties:
                    kebab-case:
                      type: string
                    snake_case:
                      type: string
                    camelCase:
                      type: string
                    PascalCase:
                      type: string
                # An object with fields of every scalar type except byte and binary
                MyScalarsObject:
                  type: object
                  properties:
                    a:
                      type: boolean
                    b:
                      type: integer
                    c:
                      type: integer
                      format: int32
                    d:
                      type: integer
                      format: int64
                    e:
                      type: number
                    f:
                      type: number
                      format: double
                    g:
                      type: number
                      format: float
                    h:
                      type: string
                    i:
                      type: string
                      format: password
                    j:
                      type: string
                      format: date
                    k:
                      type: string
                      format: date-time
                MyByteAndBinaryObject:
                  type: object
                  properties:
                    a:
                      type: string
                      format: byte
                    b:
                      type: string
                      format: binary
                # An object with an in-lined object definition
                MyObject2:
                  type: object
                  properties:
                    foo:
                      type: object
                      properties:
                        bar:
                          type: string
                # An alias for a scalar type.
                MyScalarAlias:
                  type: string
                # An alias for some other component.
                MyRefAlias:
                  $ref: '#/components/schemas/MyObject2'
                # An alias for arrays of a referenced type.
                MyRefArrayAlias:
                  type: array
                  items:
                    $ref: "#/components/schemas/MyObject2"
                # An alias for arrays of a scalar type.
                MyScalarArrayAlias:
                  type: array
                  items:
                    type: number
                # An alias for arrays of an in-lined object type
                MyInlineObjectArrayAlias:
                  type: array
                  items:
                    type: object
                    properties:
                      foo:
                        type: string
                # An alias for composite arrays of a referenced type
                MyCompositeRefArrayAlias:
                  type: array
                  items:
                    type: array
                    items:
                      $ref: '#/components/schemas/MyObject2'
                # An alias for composite arrays of a scalar type
                MyCompositeScalarArrayAlias:
                  type: array
                  items:
                    type: array
                    items:
                      type: string
                # An alias for composite arrays of an in-lined object type
                MyCompositeInlineObjectArrayAlias:
                  type: array
                  items:
                    type: array
                    items:
                      type: object
                      properties:
                        foo:
                          type: string
                RequestAndResponseRequest:
                  type: object
                  properties:
                    foo:
                      type: string
                RequestAndResponseResponse:
                  type: object
                  properties:
                    foo:
                      type: string
                RGB:
                  type: object
                  properties:
                    r:
                      type: integer
                      format: int32
                    g:
                      type: integer
                      format: int32
                    b:
                      type: integer
                      format: int32
            """);
  }

  @AfterAll
  static void afterAll() throws Exception {
    deleteGeneratedSources();
  }

  @ParameterizedTest
  @MethodSource("parameterSource")
  public void toJson(TestParameter params) throws Exception {
    var actualJson = MAPPER.readValue(MAPPER.writeValueAsString(params.asObject), JsonNode.class);
    var expectedJson = MAPPER.readValue(params.asJson, JsonNode.class);
    assertEquals(expectedJson, actualJson);
  }

  @ParameterizedTest
  @MethodSource("parameterSource")
  public void fromJson(TestParameter params) throws Exception {
    assertEquals(params.asObject, MAPPER.readValue(params.asJson, params.type));
  }

  private record TestParameter(String asJson, Object asObject, Class<?> type) {}

  public static Stream<Arguments> parameterSource() throws Exception {
    return Stream.of(
            parameterCaseObject(),
            myScalarsObject(),
            myByteAndBinaryObject(),
            myObject2TestParameter(),
            myRefAliasTestParameter(),
            myScalarAliasTestParameter(),
            myRefArrayAliasTestParameter(),
            myScalarArrayAliasTestParameter(),
            myInlineObjectArrayAliasTestParameter(),
            myCompositeRefArrayAliasTestParameter(),
            myCompositeScalarArrayAliasTestParameter(),
            myCompositeInlineObjectArrayAliasTestParameter())
        .map(x -> arguments(Named.of(x.type.getSimpleName(), x)));
  }

  private static TestParameter parameterCaseObject() throws Exception {
    var clazz = Class.forName(packageName + ".ParameterCaseObject");
    return new TestParameter(
        """
        {
          "kebab-case": "value",
          "snake_case": "value",
          "camelCase": "value",
          "PascalCase": "value"
        }
        """,
        clazz
            .getConstructor(String.class, String.class, String.class, String.class)
            .newInstance("value", "value", "value", "value"),
        clazz);
  }

  private static TestParameter myScalarsObject() throws Exception {
    var myScalarsObject = Class.forName(packageName + ".MyScalarsObject");
    return new TestParameter(
        """
              {
                "a": true,
                "b": 0,
                "c": 1,
                "d": 2,
                "e": 3,
                "f": 4.1,
                "g": 5.2,
                "h": "foo",
                "i": "password",
                "j": "2021-01-01",
                "k": "2021-01-01T00:00:00.012Z"
              }
              """,
        myScalarsObject
            .getConstructor(
                Boolean.class,
                BigInteger.class,
                Integer.class,
                Long.class,
                BigDecimal.class,
                Double.class,
                Float.class,
                String.class,
                String.class,
                LocalDate.class,
                OffsetDateTime.class)
            .newInstance(
                true,
                BigInteger.ZERO,
                1,
                2L,
                BigDecimal.valueOf(3L),
                4.1d,
                5.2f,
                "foo",
                "password",
                LocalDate.of(2021, 1, 1),
                OffsetDateTime.parse("2021-01" + "-01T00:00:00.012Z")),
        myScalarsObject);
  }

  private static TestParameter myByteAndBinaryObject() throws Exception {
    var myByteAndBinaryObject = Class.forName(packageName + ".MyByteAndBinaryObject");
    return new TestParameter(
        "{ \"a\": [3], \"b\": [7]}",
        myByteAndBinaryObject
            .getConstructor(ByteBuffer.class, ByteBuffer.class)
            .newInstance(ByteBuffer.wrap(new byte[] {3}), ByteBuffer.wrap(new byte[] {7})),
        myByteAndBinaryObject);
  }

  private static TestParameter myObject2TestParameter() throws Exception {
    var myobject2 = Class.forName(packageName + ".MyObject2");
    var foo = Class.forName(packageName + ".myobject2.Foo");
    return new TestParameter(
        "{ \"foo\": { \"bar\": \"value\" } }",
        myobject2
            .getConstructor(foo)
            .newInstance(foo.getConstructor(String.class).newInstance("value")),
        myobject2);
  }

  private static TestParameter myRefAliasTestParameter() throws Exception {
    var myRefAlias = Class.forName(packageName + ".MyRefAlias");
    var myobject2 = Class.forName(packageName + ".MyObject2");
    var foo = Class.forName(packageName + ".myobject2.Foo");
    return new TestParameter(
        "{\"foo\": {\"bar\": \"value\"}}",
        myRefAlias
            .getConstructor(myobject2)
            .newInstance(
                myobject2
                    .getConstructor(foo)
                    .newInstance(foo.getConstructor(String.class).newInstance("value"))),
        myRefAlias);
  }

  private static TestParameter myScalarAliasTestParameter() throws Exception {
    var myScalarAlias = Class.forName(packageName + ".MyScalarAlias");
    return new TestParameter(
        "\"value\"",
        myScalarAlias.getMethod("creator", String.class).invoke(null, "value"),
        myScalarAlias);
  }

  private static TestParameter myRefArrayAliasTestParameter() throws Exception {
    var myRefArrayAlias = Class.forName(packageName + ".MyRefArrayAlias");
    var myObject2 = Class.forName(packageName + ".MyObject2");
    var foo = Class.forName(packageName + ".myobject2.Foo");
    return new TestParameter(
        "[{ \"foo\": { \"bar\": \"value\" } }]",
        myRefArrayAlias
            .getMethod("creator", List.class)
            .invoke(
                null,
                List.of(
                    myObject2
                        .getConstructor(foo)
                        .newInstance(foo.getConstructor(String.class).newInstance("value")))),
        myRefArrayAlias);
  }

  private static TestParameter myScalarArrayAliasTestParameter() throws Exception {
    var myScalarArrayAlias = Class.forName(packageName + ".MyScalarArrayAlias");
    return new TestParameter(
        "[123]",
        myScalarArrayAlias
            .getMethod("creator", List.class)
            .invoke(null, List.of(BigDecimal.valueOf(123))),
        myScalarArrayAlias);
  }

  private static TestParameter myInlineObjectArrayAliasTestParameter() throws Exception {
    var myInlineObjectArrayAlias = Class.forName(packageName + ".MyInlineObjectArrayAlias");
    var myInlineObjectArrayAliasItem =
        Class.forName(packageName + ".myinlineobjectarrayalias.MyInlineObjectArrayAliasItem");
    return new TestParameter(
        "[{\"foo\": \"foo\"}]",
        myInlineObjectArrayAlias
            .getMethod("creator", List.class)
            .invoke(
                null,
                List.of(
                    myInlineObjectArrayAliasItem.getConstructor(String.class).newInstance("foo"))),
        myInlineObjectArrayAlias);
  }

  private static TestParameter myCompositeRefArrayAliasTestParameter() throws Exception {
    var myCompositeRefArrayAlias = Class.forName(packageName + ".MyCompositeRefArrayAlias");
    var myObject2 = Class.forName(packageName + ".MyObject2");
    var foo = Class.forName(packageName + ".myobject2.Foo");
    return new TestParameter(
        "[[{\"foo\": { \"bar\": \"value\"}}]]",
        myCompositeRefArrayAlias
            .getMethod("creator", List.class)
            .invoke(
                null,
                List.of(
                    List.of(
                        myObject2
                            .getConstructor(foo)
                            .newInstance(foo.getConstructor(String.class).newInstance("value"))))),
        myCompositeRefArrayAlias);
  }

  private static TestParameter myCompositeScalarArrayAliasTestParameter() throws Exception {
    var myCompositeScalarArrayAlias = Class.forName(packageName + ".MyCompositeScalarArrayAlias");
    return new TestParameter(
        "[[\"foo\"]]",
        myCompositeScalarArrayAlias
            .getMethod("creator", List.class)
            .invoke(null, List.of(List.of("foo"))),
        myCompositeScalarArrayAlias);
  }

  private static TestParameter myCompositeInlineObjectArrayAliasTestParameter() throws Exception {
    var myCompositeInlineObjectArrayAlias =
        Class.forName(packageName + ".MyCompositeInlineObjectArrayAlias");
    var myCompositeInlineObjectArrayAliasItem =
        Class.forName(
            packageName
                + ".mycompositeinlineobjectarrayalias.MyCompositeInlineObjectArrayAliasItem");
    return new TestParameter(
        "[[{\"foo\": \"foo\"}]]",
        myCompositeInlineObjectArrayAlias
            .getMethod("creator", List.class)
            .invoke(
                null,
                List.of(
                    List.of(
                        myCompositeInlineObjectArrayAliasItem
                            .getConstructor(String.class)
                            .newInstance("foo")))),
        myCompositeInlineObjectArrayAlias);
  }
}
