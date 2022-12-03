package io.github.tomboyo.lily.compiler.icg;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.AstField;
import io.github.tomboyo.lily.compiler.ast.AstReference;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.util.Pair;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OasSchemaToAst {

  private static final Logger LOGGER = LoggerFactory.getLogger(OasSchemaToAst.class);

  private final PackageName basePackage;

  private OasSchemaToAst(PackageName basePackage) {
    this.basePackage = basePackage;
  }

  /**
   * Evaluate an OAS schema to AST, returning an AstReference to the top-level type generated from
   * the schema.
   *
   * @param basePackage The root package under which to generate new types.
   * @param name The name of the schema, which is used to name types evaluated from the schema.
   * @param schema The schema to evaluate to AST.
   * @return A pair describing the root reference and stream of evaluated AST.
   */
  public static Pair<AstReference, Stream<Ast>> evaluate(
      PackageName basePackage, SimpleName name, Schema<?> schema) {
    return new OasSchemaToAst(basePackage).evaluateSchema(basePackage, name, schema);
  }

  private Pair<AstReference, Stream<Ast>> evaluateSchema(
      PackageName currentPackage, SimpleName name, Schema<?> schema) {
    var type = schema.getType();
    if (type == null) {
      return new Pair<>(toBasePackageClassReference(requireNonNull(schema.get$ref())), Stream.of());
    }

    return switch (type) {
      case "integer", "number", "string", "boolean" -> new Pair<>(
          toStdLibAstReference(schema.getType(), schema.getFormat()), Stream.of());
      case "array" -> evaluateArray(currentPackage, name, (ArraySchema) schema);
      case "object" -> evaluateObject(currentPackage, name, schema);
      default -> throw new IllegalArgumentException(("Unexpected type: " + type));
    };
  }

  private AstReference toBasePackageClassReference(String $ref) {
    return AstReference.ref(
        Fqn.of(basePackage, SimpleName.of($ref.replaceFirst("^#/components/schemas/", ""))),
        List.of());
  }

  private AstReference toStdLibAstReference(String type, String format) {
    switch (type) {
      case "integer":
        if (format == null) {
          return StdlibAstReferences.astBigInteger();
        }

        return switch (format) {
          case "int64" -> StdlibAstReferences.astLong();
          case "int32" -> StdlibAstReferences.astInteger();
          default -> defaultForUnsupportedFormat(type, format, StdlibAstReferences.astBigInteger());
        };
      case "number":
        if (format == null) {
          return StdlibAstReferences.astBigDecimal();
        }

        return switch (format) {
          case "double" -> StdlibAstReferences.astDouble();
          case "float" -> StdlibAstReferences.astFloat();
          default -> defaultForUnsupportedFormat(type, format, StdlibAstReferences.astBigDecimal());
        };
      case "string":
        if (format == null) {
          return StdlibAstReferences.astString();
        }

        return switch (format) {
          case "password" -> StdlibAstReferences.astString();
          case "byte", "binary" -> StdlibAstReferences.astByteBuffer();
          case "date" -> StdlibAstReferences.astLocalDate();
          case "date-time" -> StdlibAstReferences.astOffsetDateTime();
          default -> defaultForUnsupportedFormat(type, format, StdlibAstReferences.astString());
        };
      case "boolean":
        if (format == null) {
          return StdlibAstReferences.astBoolean();
        } else {
          return defaultForUnsupportedFormat(type, format, StdlibAstReferences.astBoolean());
        }
      default:
        throw new IllegalArgumentException("Unexpected type: " + type);
    }
  }

  private AstReference defaultForUnsupportedFormat(
      String type, String format, AstReference defaultAst) {
    if (format != null) {
      LOGGER.warn("Using default class for unsupported format: type={} format={}", type, format);
    }
    return defaultAst;
  }

  private Pair<AstReference, Stream<Ast>> evaluateArray(
      PackageName currentPackage, SimpleName name, ArraySchema arraySchema) {
    if ("object".equals(arraySchema.getItems().getType())) {
      /*
       AST generated from objects in arrays are named similarly to all other objects in that they
       are defined withi subordinate packages of superior types (if any). They are also given the "Item" suffix. For
       example:

       1. Suppose the #/components/Cats component is an array containing [arrays of] objects. If
       the base package is p, then the resulting AstReference should be:

          List<[List<...<]p.CatsItem[>...>]>

       2. Suppose the #/components/Bus component is an object with a "wheels" array parameter
       which contains [arrays of] objects. Then the resulting AstReference should be:

          List<[List<...<]p.bus.WheelsItem[>...>]>

       Regardless of how many arrays are nested within an array, the Item suffix as appended only
       once to the referent type name. I.e. we do not generate WheelsItemItem...Item. Furthermore, since arrays do not
       generate their own
       types (for exceptions, see OasComponentsToAst), nested arrays do not affect the package
       name of the referent
       type.
      */
      var interior = evaluateSchema(currentPackage, name.resolve("Item"), arraySchema.getItems());
      return new Pair<>(StdlibAstReferences.astListOf(interior.left()), interior.right());
    } else {
      // All types other than Object do not result in new AST, so we do not use the naming strategy
      // used for objects.
      var interior = evaluateSchema(currentPackage, name, arraySchema.getItems());
      return new Pair<>(StdlibAstReferences.astListOf(interior.left()), interior.right());
    }
  }

  private Pair<AstReference, Stream<Ast>> evaluateObject(
      PackageName currentPackage, SimpleName name, Schema<?> schema) {
    /*
     Generate the AST required to define the fields of this new class. If we define any classes
     for our fields, we
     place them in a subordinate package named after this class. For example, if we define the
     class package.MyClass,
     then any classes defined for MyClass' fields are defined under the package.myclass package.
    */
    var properties = Optional.ofNullable(schema.getProperties()).orElse(Map.of());
    var interiorPackage = currentPackage.resolve(name.toString());
    var fieldAndAst =
        properties.entrySet().stream()
            .map(
                entry -> {
                  var jsonName = entry.getKey();
                  var fieldSchema = entry.getValue();
                  var fieldPackage =
                      fieldSchema.get$ref() == null
                          ? interiorPackage
                          : basePackage; // $ref's always point to a base package type.
                  var refAndAst =
                      evaluateSchema(fieldPackage, SimpleName.of(jsonName), fieldSchema);
                  return refAndAst.mapLeft(
                      ref -> new AstField(ref, SimpleName.of(jsonName), jsonName));
                })
            .toList();

    var exteriorClass =
        AstClass.of(
            Fqn.of(currentPackage, name), fieldAndAst.stream().map(Pair::left).collect(toList()));
    var interiorAst = fieldAndAst.stream().flatMap(Pair::right);
    return new Pair<>(
        AstReference.ref(Fqn.of(currentPackage, name), List.of()),
        Stream.concat(Stream.of(exteriorClass), interiorAst));
  }
}
