package io.github.tomboyo.lily.compiler.icg;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.AstClassAlias;
import io.github.tomboyo.lily.compiler.ast.AstField;
import io.github.tomboyo.lily.compiler.ast.AstReference;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OasSchemaToAst {

  private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(OasSchemaToAst.class);

  private final Logger logger;
  private final String basePackage;

  private OasSchemaToAst(Logger logger, String basePackage) {
    this.logger = logger;
    this.basePackage = basePackage;
  }

  public static Stream<Ast> evaluate(String basePackage, Map<String, Schema> schema) {
    return evaluate(DEFAULT_LOGGER, basePackage, schema);
  }

  public static Stream<Ast> evaluate(
      Logger logger, String basePackage, Map<String, Schema> schema) {
    return new OasSchemaToAst(logger, basePackage).evaluate(schema);
  }

  private Stream<Ast> evaluate(Map<String, Schema> schema) {
    return schema.entrySet().stream()
        .flatMap(entry -> evaluateRootSchema(entry.getKey(), entry.getValue()));
  }

  private Stream<Ast> evaluateRootSchema(String schemaName, Schema<?> schema) {
    var type = schema.getType();
    if (type == null) {
      return Stream.of(evaluateRootRef(basePackage, schemaName, schema));
    }

    return switch (type) {
      case "integer", "number", "string", "boolean" -> Stream.of(
          evaluateRootScalar(basePackage, schemaName, schema));
      case "array" -> evaluateRootArray(basePackage, schemaName, (ArraySchema) schema);
      case "object" -> evaluateInteriorComponent(basePackage, schemaName, schema);
      default -> throw new IllegalArgumentException(("Unexpected type: " + type));
    };
  }

  private AstClassAlias evaluateRootRef(
      String currentPackage, String schemaName, Schema<?> schema) {
    return new AstClassAlias(
        currentPackage, schemaName, toBasePackageClassReference(requireNonNull(schema.get$ref())));
  }

  /**
   * Evaluate a root scalar component (i.e. not an object or array) to an alias of some stdlib type.
   */
  private AstClassAlias evaluateRootScalar(
      String currentPackage, String schemaName, Schema<?> schema) {
    var type = schema.getType();
    var format = schema.getFormat();
    return new AstClassAlias(currentPackage, schemaName, toStdLibAstReference(type, format));
  }

  /** Evaluate a root array component to an alias of {@code List<T>} type. */
  private Stream<Ast> evaluateRootArray(
      String currentPackage, String schemaName, ArraySchema schema) {
    var itemType = schema.getItems().getType();
    if (itemType == null) {
      return evaluateRootRefArray(currentPackage, schemaName, schema);
    } else {
      return switch (itemType) {
        case "integer", "number", "string", "boolean" -> evaluateRootScalarArray(
            currentPackage, schemaName, schema);
        case "array" -> evaluateRootCompositeArray(currentPackage, schemaName, schema);
        case "object" -> evaluateRootInlineObjectArray(currentPackage, schemaName, schema);
        default -> throw new IllegalArgumentException("Unexpected type: " + itemType);
      };
    }
  }

  /** Evaluate a root array whose items are a component $ref to an alias of the referenced type. */
  private Stream<Ast> evaluateRootRefArray(
      String currentPackage, String schemaName, ArraySchema schema) {
    var ref = requireNonNull(schema.getItems().get$ref());
    return Stream.of(
        new AstClassAlias(
            currentPackage,
            schemaName,
            StdlibAstReferences.astListOf(toBasePackageClassReference(ref))));
  }

  /**
   * Evaluate a root array whose items are a scalar to an alias of {@code List<S>}, where {@code S}
   * is the scalar type.
   */
  private Stream<Ast> evaluateRootScalarArray(
      String currentPackage, String schemaName, ArraySchema schema) {
    var itemType = schema.getItems().getType();
    var itemFormat = schema.getItems().getFormat();
    return Stream.of(
        new AstClassAlias(
            currentPackage,
            schemaName,
            StdlibAstReferences.astListOf(toStdLibAstReference(itemType, itemFormat))));
  }

  /**
   * Evaluate a root array-of-arrays into an alias of a composite list {@code
   * List<List<...List<T>...>>}.
   */
  private Stream<Ast> evaluateRootCompositeArray(
      String currentPackage, String schemaName, ArraySchema schema) {
    var interiorSchema = getFirstNonArrayChildSchema(schema);

    AstReference astReference; // The `T` in List<List<...List<T>...>>
    Stream<Ast> interiorAst; // The definition of `T` and subordinate types
    var type = interiorSchema.getType();
    if (type == null) {
      var ref = requireNonNull(interiorSchema.get$ref());
      interiorAst = Stream.of();
      astReference = toBasePackageClassReference(ref);
    } else {
      var format = interiorSchema.getFormat();
      switch (type) {
        case "integer", "number", "string", "boolean" -> {
          interiorAst = Stream.of();
          astReference = toStdLibAstReference(type, format);
        }
        case "object" -> {
          var interiorPackage = Support.joinPackages(currentPackage, schemaName.toLowerCase());
          var interiorTypeName = schemaName + "Item";
          interiorAst = evaluateInteriorObject(interiorPackage, interiorTypeName, interiorSchema);
          astReference = new AstReference(interiorPackage, interiorTypeName);
        }
        default -> throw new IllegalArgumentException("Unexpected type: " + type);
      }
    }

    for (int i = 0; i < numberOfArrayChildren(schema); i += 1) {
      astReference = StdlibAstReferences.astListOf(astReference);
    }

    var alias =
        new AstClassAlias(currentPackage, schemaName, StdlibAstReferences.astListOf(astReference));

    return Stream.concat(Stream.of(alias), interiorAst);
  }

  private Schema<?> getFirstNonArrayChildSchema(ArraySchema root) {
    Schema<?> current = root;
    while ("array".equals(current.getType())) {
      current = ((ArraySchema) current).getItems();
    }
    return current;
  }

  private int numberOfArrayChildren(ArraySchema root) {
    int result = 0;
    Schema<?> current = root.getItems();
    while ("array".equals(current.getType())) {
      current = ((ArraySchema) current).getItems();
      result += 1;
    }
    return result;
  }

  /**
   * Evaluate a root array whose items are in-line defined objects into an alias of {@code List<T>},
   * where {@code T} is the in-line object type.
   */
  private Stream<Ast> evaluateRootInlineObjectArray(
      String currentPackage, String schemaName, ArraySchema schema) {
    var itemPackage = Support.joinPackages(currentPackage, schemaName.toLowerCase());
    var itemName = schemaName + "Item";
    var objectSchema = schema.getItems();
    var inlineAst = evaluateInteriorObject(itemPackage, itemName, objectSchema);
    var aliasAst =
        new AstClassAlias(
            currentPackage,
            schemaName,
            StdlibAstReferences.astListOf(new AstReference(itemPackage, itemName)));
    return Stream.concat(Stream.of(aliasAst), inlineAst);
  }

  /** Evaluate any interior (i.e. non-root) component. */
  private Stream<Ast> evaluateInteriorComponent(
      String currentPackage, String schemaName, Schema<?> schema) {
    var type = schema.getType();
    if (type == null) {
      return Stream.of();
    }

    return switch (type) {
      case "integer", "number", "string", "boolean" -> Stream.of();
      case "object" -> evaluateInteriorObject(currentPackage, schemaName, schema);
      case "array" -> evaluateInteriorArray(currentPackage, schemaName, (ArraySchema) schema);
      default -> throw new IllegalArgumentException("Unexpected type: " + type);
    };
  }

  /** Evaluate any interior (e.g. non-root) component of array type. */
  private Stream<Ast> evaluateInteriorArray(
      String currentPackage, String schemaName, ArraySchema schema) {
    var itemSchema = schema.getItems();
    var itemType = itemSchema.getType();

    // Only append "Item" to the name if the nested type is actually an object. Otherwise, we end up
    // with
    // ThingItemItem...Item in the presence of nested array definitions.
    if ("object".equals(itemType)) {
      return evaluateInteriorComponent(currentPackage, schemaName + "Item", itemSchema);
    } else {
      return evaluateInteriorComponent(currentPackage, schemaName, itemSchema);
    }
  }

  /** Evaluate any interior (e.g. non-root) component of object type */
  private Stream<Ast> evaluateInteriorObject(
      String currentPackage, String schemaName, Schema<?> schema) {
    // This package is "nested" beneath this class. Any nested in-line class definitions are
    // generated within the interior package.
    var interiorPackage = Support.joinPackages(currentPackage, schemaName.toLowerCase());

    // 1. Define a new class for this object.
    Map<String, Schema> properties = Optional.ofNullable(schema.getProperties()).orElse(Map.of());
    var fields =
        properties.entrySet().stream()
            .map(
                entry -> {
                  var pName = entry.getKey();
                  var pSchema = entry.getValue();
                  var reference = toReference(interiorPackage, pName, pSchema);
                  return new AstField(reference, pName);
                })
            .collect(toList());
    var exteriorClass = new AstClass(currentPackage, schemaName, fields);

    // 2. Define new classes for interior objects (e.g. in-line object definitions for our fields).
    // Note that many properties _may not_ warrant AST generation -- those return an empty stream.
    var interiorClasses =
        properties.entrySet().stream()
            .flatMap(
                entry ->
                    evaluateInteriorComponent(
                        interiorPackage,
                        Support.capitalCamelCase(entry.getKey()),
                        entry.getValue()));

    return Stream.concat(Stream.of(exteriorClass), interiorClasses);
  }

  private AstReference toReference(String referentPackage, String schemaName, Schema<?> schema) {
    var type = schema.getType();
    var format = schema.getFormat();
    var ref = schema.get$ref();

    if (type == null) {
      return toBasePackageClassReference(ref);
    }

    return switch (type) {
      case "integer", "number", "string", "boolean" -> toStdLibAstReference(type, format);
      case "array" -> toListReference(referentPackage, schemaName, schema);
      case "object" -> new AstReference(referentPackage, schemaName);
      default -> throw new IllegalArgumentException("Unexpected type: " + type);
    };
  }

  private AstReference toBasePackageClassReference(String $ref) {
    return new AstReference(basePackage, $ref.replaceFirst("^#/components/schemas/", ""));
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
          case "byte", "binary" -> StdlibAstReferences.astByteArray();
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
      logger.warn("Using default class for unsupported format: type={} format={}", type, format);
    }
    return defaultAst;
  }

  private AstReference toListReference(
      String referentPackage, String schemaName, Schema<?> schema) {
    var itemSchema = ((ArraySchema) schema).getItems();
    var itemName = schemaName + "Item";
    return new AstReference(
        "java.util", "List", List.of(toReference(referentPackage, itemName, itemSchema)));
  }
}
