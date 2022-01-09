package com.github.tomboyo.lily.ast;

import static com.github.tomboyo.lily.ast.StdlibAstReferences.astBigDecimal;
import static com.github.tomboyo.lily.ast.StdlibAstReferences.astBigInteger;
import static com.github.tomboyo.lily.ast.StdlibAstReferences.astBoolean;
import static com.github.tomboyo.lily.ast.StdlibAstReferences.astByteArray;
import static com.github.tomboyo.lily.ast.StdlibAstReferences.astDouble;
import static com.github.tomboyo.lily.ast.StdlibAstReferences.astFloat;
import static com.github.tomboyo.lily.ast.StdlibAstReferences.astInteger;
import static com.github.tomboyo.lily.ast.StdlibAstReferences.astListOf;
import static com.github.tomboyo.lily.ast.StdlibAstReferences.astLocalDate;
import static com.github.tomboyo.lily.ast.StdlibAstReferences.astLong;
import static com.github.tomboyo.lily.ast.StdlibAstReferences.astOffsetDateTime;
import static com.github.tomboyo.lily.ast.StdlibAstReferences.astString;
import static com.github.tomboyo.lily.ast.Support.capitalCamelCase;
import static com.github.tomboyo.lily.ast.Support.joinPackages;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

import com.github.tomboyo.lily.ast.type.Ast;
import com.github.tomboyo.lily.ast.type.AstClass;
import com.github.tomboyo.lily.ast.type.AstClassAlias;
import com.github.tomboyo.lily.ast.type.AstField;
import com.github.tomboyo.lily.ast.type.AstReference;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OasSchemaToAst {

  private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(OasSchemaToAst.class);

  private static record Constants(Logger logger, String basePackage) {}

  public static Stream<Ast> evaluateComponents(String basePackage, Components components) {
    return evaluateComponents(DEFAULT_LOGGER, basePackage, components);
  }

  public static Stream<Ast> evaluateComponents(
      Logger logger, String basePackage, Components components) {
    return components.getSchemas().entrySet().stream()
        .flatMap(
            entry ->
                evaluateRootComponent(
                    new Constants(logger, basePackage), entry.getKey(), entry.getValue()));
  }

  private static Stream<Ast> evaluateRootComponent(
      Constants constants, String schemaName, Schema schema) {
    var type = schema.getType();
    if (type == null) {
      // TODO: Alias of $ref
      return Stream.of();
    }

    switch (type) {
      case "integer":
      case "number":
      case "string":
      case "boolean":
        return Stream.of(
            evaluateRootScalar(constants, constants.basePackage(), schemaName, schema));
      case "array":
        return evaluateRootArray(
            constants, constants.basePackage(), schemaName, (ArraySchema) schema);
      case "object":
        return evaluateInteriorComponent(constants, constants.basePackage(), schemaName, schema);
      default:
        throw new IllegalArgumentException(("Unexpected type: " + type));
    }
  }

  /**
   * Evaluate a root scalar component (i.e. not an object or array) to an alias of some stdlib type.
   */
  private static AstClassAlias evaluateRootScalar(
      Constants constants, String currentPackage, String schemaName, Schema schema) {
    var type = schema.getType();
    var format = schema.getFormat();
    return new AstClassAlias(
        currentPackage, schemaName, toStdLibAstReference(constants, type, format));
  }

  /** Evaluate a root array component to an alias of {@code List<T>} type. */
  private static Stream<Ast> evaluateRootArray(
      Constants constants, String currentPackage, String schemaName, ArraySchema schema) {
    var itemType = schema.getItems().getType();
    if (itemType == null) {
      return evaluateRootRefArray(constants, currentPackage, schemaName, schema);
    } else {
      switch (itemType) {
        case "integer":
        case "number":
        case "string":
        case "boolean":
          return evaluateRootScalarArray(constants, currentPackage, schemaName, schema);
        case "array":
          return evaluateRootCompositeArray(constants, currentPackage, schemaName, schema);
        case "object":
          return evaluateRootInlineObjectArray(constants, currentPackage, schemaName, schema);
        default:
          throw new IllegalArgumentException(("Unexpected type: " + itemType));
      }
    }
  }

  /** Evaluate a root array whose items are a component $ref to an alias of the referenced type. */
  private static Stream<Ast> evaluateRootRefArray(
      Constants constants, String currentPackage, String schemaName, ArraySchema schema) {
    var ref = requireNonNull(schema.getItems().get$ref());
    return Stream.of(
        new AstClassAlias(
            currentPackage, schemaName, astListOf(toBasePackageClassReference(constants, ref))));
  }

  /**
   * Evaluate a root array whose items are a scalar to an alias of {@code List<S>}, where {@code S}
   * is the scalar type.
   */
  private static Stream<Ast> evaluateRootScalarArray(
      Constants constants, String currentPackage, String schemaName, ArraySchema schema) {
    var itemType = schema.getItems().getType();
    var itemFormat = schema.getItems().getFormat();
    return Stream.of(
        new AstClassAlias(
            currentPackage,
            schemaName,
            astListOf(toStdLibAstReference(constants, itemType, itemFormat))));
  }

  /**
   * Evaluate a root array-of-arrays into an alias of a composite list {@code
   * List<List<...List<T>...>>}.
   */
  private static Stream<Ast> evaluateRootCompositeArray(
      Constants constants, String currentPackage, String schemaName, ArraySchema schema) {
    // schema: points to the top-level array alias component
    // schemaBackPath: contains the array-typed child components of schema, in reverse hierarchical
    // order.
    // currentNode: after the loop exits, this holds the non-array child.
    var schemaBackPath = new ArrayList<ArraySchema>();
    var currentNode = schema.getItems();
    while ("array".equals(currentNode.getType())) {
      var currentArraySchema = (ArraySchema) currentNode;
      schemaBackPath.add(currentArraySchema);
      currentNode = currentArraySchema.getItems();
    }

    AstReference astReference;
    Stream<Ast> innerTypes;
    var type = currentNode.getType();
    if (type == null) {
      var ref = requireNonNull(currentNode.get$ref());
      innerTypes = Stream.of();
      astReference = toBasePackageClassReference(constants, ref);
    } else {
      var format = currentNode.getFormat();
      // current node is not an array schema.
      switch (currentNode.getType()) {
        case "integer":
        case "number":
        case "string":
        case "boolean":
          innerTypes = Stream.of();
          astReference = toStdLibAstReference(constants, type, format);
          break;
        case "object":
          var innerTypePackage = joinPackages(currentPackage, schemaName.toLowerCase());
          var innerTypeName = schemaName + "Item";
          innerTypes =
              evaluateInteriorObject(constants, innerTypePackage, innerTypeName, currentNode);
          astReference = new AstReference(innerTypePackage, innerTypeName);
          break;
        default:
          throw new IllegalArgumentException(("Unexpected type: " + type));
      }
    }

    // Use the back-reference list to construct the composite astReference chain (List of list of
    // list of Foo).
    // A list of this result is what we are creating an alias of.
    for (var backSchema : schemaBackPath) {
      astReference = astListOf(astReference);
    }

    var alias = new AstClassAlias(currentPackage, schemaName, astListOf(astReference));

    return Stream.concat(Stream.of(alias), innerTypes);
  }

  /**
   * Evaluate a root array whose items are in-line defined objects into an alias of {@code List<T>},
   * where {@code T} is the in-line object type.
   */
  private static Stream<Ast> evaluateRootInlineObjectArray(
      Constants constants, String currentPackage, String schemaName, ArraySchema schema) {
    var itemPackage = joinPackages(currentPackage, schemaName.toLowerCase());
    var itemName = schemaName + "Item";
    var objectSchema = schema.getItems();
    var inlineAst = evaluateInteriorObject(constants, itemPackage, itemName, objectSchema);
    var aliasAst =
        new AstClassAlias(
            currentPackage, schemaName, astListOf(new AstReference(itemPackage, itemName)));
    return Stream.concat(Stream.of(aliasAst), inlineAst);
  }

  /** Evaluate any interior (i.e. non-root) component. */
  private static Stream<Ast> evaluateInteriorComponent(
      Constants constants, String currentPackage, String schemaName, Schema schema) {
    var type = schema.getType();
    if (type == null) {
      return Stream.of();
    }

    switch (type) {
      case "integer":
      case "number":
      case "string":
      case "boolean":
        return Stream.of();
      case "object":
        return evaluateInteriorObject(constants, currentPackage, schemaName, schema);
      case "array":
        return evaluateInteriorArray(constants, currentPackage, schemaName, schema);
      default:
        throw new IllegalArgumentException(("Unexpected type: " + type));
    }
  }

  /** Evaluate any interior (e.g. non-root) component of array type. */
  private static Stream<Ast> evaluateInteriorArray(
      Constants constants, String currentPackage, String schemaName, Schema schema) {
    var itemSchema = ((ArraySchema) schema).getItems();
    var itemType = itemSchema.getType();

    // Only append "Item" to the name if the nested type is actually an object. Otherwise, we end up
    // with
    // ThingItemItem...Item in the presence of nested array definitions.
    if ("object".equals(itemType)) {
      return evaluateInteriorComponent(constants, currentPackage, schemaName + "Item", itemSchema);
    } else {
      return evaluateInteriorComponent(constants, currentPackage, schemaName, itemSchema);
    }
  }

  /** Evaluate any interior (e.g. non-root) component of object type */
  private static Stream<Ast> evaluateInteriorObject(
      Constants constants, String currentPackage, String schemaName, Schema schema) {
    // This package is "nested" beneath this class. Any nested in-line class definitions are
    // generated within the interior package.
    var interiorPackage = joinPackages(currentPackage, schemaName.toLowerCase());

    // 1. Define a new class for this object.
    Map<String, Schema> properties = Optional.ofNullable(schema.getProperties()).orElse(Map.of());
    var fields =
        properties.entrySet().stream()
            .map(
                entry -> {
                  var pName = entry.getKey();
                  var pSchema = entry.getValue();
                  var reference = toReference(constants, interiorPackage, pName, pSchema);
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
                        constants,
                        interiorPackage,
                        capitalCamelCase(entry.getKey()),
                        entry.getValue()));

    return Stream.concat(Stream.of(exteriorClass), interiorClasses);
  }

  private static AstReference toReference(
      Constants constants, String referentPackage, String schemaName, Schema schema) {
    var type = schema.getType();
    var format = schema.getFormat();
    var ref = schema.get$ref();

    if (type == null) {
      return toBasePackageClassReference(constants, ref);
    }

    switch (type) {
      case "integer":
      case "number":
      case "string":
      case "boolean":
        return toStdLibAstReference(constants, type, format);
      case "array":
        return toListReference(constants, referentPackage, schemaName, schema);
      case "object":
        return new AstReference(referentPackage, schemaName);
      default:
        throw new IllegalArgumentException("Unexpected type: " + type);
    }
  }

  private static AstReference toBasePackageClassReference(Constants constants, String $ref) {
    return new AstReference(
        constants.basePackage(), $ref.replaceFirst("^#/components/schemas/", ""));
  }

  private static AstReference toStdLibAstReference(
      Constants constants, String type, String format) {
    switch (type) {
      case "integer":
        if (format == null) {
          return astBigInteger();
        }

        switch (format) {
          case "int64":
            return astLong();
          case "int32":
            return astInteger();
          default:
            return defaultForUnsupportedFormat(constants, type, format, astBigInteger());
        }
      case "number":
        if (format == null) {
          return astBigDecimal();
        }

        switch (format) {
          case "double":
            return astDouble();
          case "float":
            return astFloat();
          default:
            return defaultForUnsupportedFormat(constants, type, format, astBigDecimal());
        }
      case "string":
        if (format == null) {
          return astString();
        }

        switch (format) {
          case "password":
            return astString();
          case "byte":
          case "binary":
            return astByteArray();
          case "date":
            return astLocalDate();
          case "date-time":
            return astOffsetDateTime();
          default:
            return defaultForUnsupportedFormat(constants, type, format, astString());
        }
      case "boolean":
        if (format == null) {
          return astBoolean();
        } else {
          return defaultForUnsupportedFormat(constants, type, format, astBoolean());
        }
      default:
        throw new IllegalArgumentException("Unexpected type: " + type);
    }
  }

  private static AstReference defaultForUnsupportedFormat(
      Constants constants, String type, String format, AstReference defaultAst) {
    if (format != null) {
      constants
          .logger()
          .warn("Using default class for unsupported format: type={} format={}", type, format);
    }
    return defaultAst;
  }

  private static AstReference toListReference(
      Constants constants, String referentPackage, String schemaName, Schema schema) {
    var itemSchema = ((ArraySchema) schema).getItems();
    var itemName = schemaName + "Item";
    return new AstReference(
        "java.util",
        "List",
        List.of(toReference(constants, referentPackage, itemName, itemSchema)));
  }
}
