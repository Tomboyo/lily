package com.github.tomboyo.lily.ast;

import com.github.tomboyo.lily.ast.type.AstClass;
import com.github.tomboyo.lily.ast.type.AstReference;
import com.github.tomboyo.lily.ast.type.Field;
import com.github.tomboyo.lily.ast.type.NewPackage;
import com.github.tomboyo.lily.ast.type.PackageContents;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class OasSchemaToAst {

  private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(OasSchemaToAst.class);

  public static Stream<PackageContents> generateAst(
      String currentPackage,
      String schemaName,
      Schema schema
  ) {
    return generateAst(DEFAULT_LOGGER, currentPackage, schemaName, schema);
  }

  public static Stream<PackageContents> generateAst(
      Logger logger,
      String currentPackage,
      String schemaName,
      Schema schema) {
    var type =
        Optional.ofNullable(schema.getType())
            .map(String::toLowerCase)
            .orElse(null);

    return switch (type) {
      case null, "integer", "number", "string", "boolean" -> Stream.of();
      case "object" -> generateObjectAst(logger, currentPackage, schemaName, schema);
      case "array" -> generateListAst(currentPackage,
          schemaName + "Item",
          schema);
      default -> throw new IllegalArgumentException(("Unexpected type: " +
          type));
    };
  }

  private static Stream<PackageContents> generateListAst(String currentPackage,
      String schemaName,
      Schema schema) {
    var itemSchema = ((ArraySchema) schema).getItems();
    var itemType = itemSchema.getType();

    // Only append "Item" to the name if the nested type is actually an object. Otherwise, we end up with
    // ThingItemItem...Item in the presence of nested array definitions.
    if (itemType != null && itemType.equalsIgnoreCase("object")) {
      return generateAst(currentPackage, schemaName + "Item", itemSchema);
    } else {
      return generateAst(currentPackage, schemaName, itemSchema);
    }
  }

  private static Stream<PackageContents> generateObjectAst(Logger logger, String currentPackage,
      String schemaName,
      Schema schema) {
    // This package is "nested" beneath this class. Any nested in-line class definitions are generated within the
    // interior package.
    var interiorPackage =
        String.join(".", currentPackage, schemaName.toLowerCase());

    // 1. Define a new class for this object.
    Map<String, Schema> properties = schema.getProperties();
    var fields = properties.entrySet().stream().map(entry -> {
      var pName = entry.getKey();
      var pSchema = entry.getValue();
      var reference = toReference(logger, interiorPackage, pName, pSchema);
      return new Field(reference, pName);
    }).collect(toList());
    var exteriorClass = new AstClass(schemaName, fields);

    // 2. Define new classes for interior objects (e.g. in-line object definitions for our fields).
    // Note that many properties _may not_ warrant AST generation -- those return an empty stream.
    var interiorClasses =
        properties.entrySet()
            .stream()
            .flatMap(entry -> generateAst(interiorPackage,
                toClassCase(entry.getKey()),
                entry.getValue())).collect(Collectors.toSet());

    if (interiorClasses.isEmpty()) {
      return Stream.of(exteriorClass);
    } else {
      return Stream.of(exteriorClass, new NewPackage(interiorPackage, interiorClasses));
    }
  }

  private static String toClassCase(String name) {
    return name.substring(0, 1).toUpperCase() + name.substring(1);
  }

  private static AstReference toReference(
      Logger logger,
      String packageName,
      String schemaName,
      Schema schema) {
    var type = schema.getType();
    var format = schema.getFormat();
    var ref = schema.get$ref();

    return switch (type) {
      case null -> toBasePackageClassReference(ref);
      case "integer", "number", "string", "boolean" -> toStdLibAstReference(logger, type,
          format);
      case "array" -> toListReference(logger, packageName, schemaName, schema);
      case "object" -> new AstReference(packageName, schemaName);
      default -> throw new IllegalArgumentException("Unexpected type: " + type);
    };
  }

  private static AstReference toBasePackageClassReference(String $ref) {
    return new AstReference("",
        $ref.replaceFirst("^#/components/schemas/", ""));
  }

  private static AstReference toStdLibAstReference(Logger logger, String type, String format) {
    return switch (type) {
      case "integer" -> switch (format) {
        case null -> new AstReference("java.math", "BigInteger");
        case "int64" -> new AstReference("java.lang", "Long");
        case "int32" -> new AstReference("java.lang", "Integer");
        default -> defaultType(logger, type, format, new AstReference("java.math", "BigInteger"));
      };
      case "number" -> switch (format) {
        case null -> new AstReference("java.math", "BigDecimal");
        case "double" -> new AstReference("java.lang", "Double");
        case "float" -> new AstReference("java.lang", "Float");
        default -> defaultType(logger, type, format, new AstReference("java.math", "BigDecimal"));
      };
      case "string" -> switch (format) {
        case null, "password" -> new AstReference("java.lang", "String");
        case "byte", "binary" -> new AstReference("java.lang", "Byte[]");
        case "date" -> new AstReference("java.time", "LocalDate");
        case "date-time" -> new AstReference("java.time", "ZonedDateTime");
        default -> defaultType(logger, type,format,new AstReference("java.lang","String"));
      };
      case "boolean" -> switch (format) {
        case null -> new AstReference("java.lang", "Boolean");
        default -> defaultType(logger, type, format, new AstReference("java.lang", "Boolean"));
      };
      default -> throw new IllegalArgumentException("Unexpected type: " + type);
    };
  }

  private static AstReference defaultType(Logger logger, String type, String format, AstReference defaultAst) {
    if (format != null) {
      logger.warn("Using default class for unsupported format: type={} format={}", type, format);
    }
    return defaultAst;
  }

  private static AstReference toListReference(Logger logger, String packageName,
      String schemaName,
      Schema schema) {
    var itemSchema = ((ArraySchema) schema).getItems();
    var itemName = schemaName + "Item";
    return new AstReference("java.util",
        "List",
        List.of(toReference(logger, packageName, itemName, itemSchema)));
  }

  /**
   * True if this schema or any nested schema contains an object type
   */
  private static boolean containsObjectSchema(Schema schema) {
    var type = schema.getType();

    if (type == null) {
      return false;
    }

    return switch (type.toLowerCase()) {
      case "integer", "number", "string", "boolean" -> false;
      case "object" -> true;
      case "array" -> containsObjectSchema(((ArraySchema) schema).getItems());
      default -> throw new IllegalArgumentException("Unexpected type: " + type);
    };
  }
}