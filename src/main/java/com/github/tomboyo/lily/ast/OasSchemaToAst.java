package com.github.tomboyo.lily.ast;

import com.github.tomboyo.lily.ast.type.AstClass;
import com.github.tomboyo.lily.ast.type.AstReference;
import com.github.tomboyo.lily.ast.type.Field;
import com.github.tomboyo.lily.ast.type.NewPackage;
import com.github.tomboyo.lily.ast.type.PackageContents;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class OasSchemaToAst {

  // ignoring special case for alias types at the moment...
  public static Stream<PackageContents> generateAst(
      String currentPackage,
      String schemaName,
      Schema schema) {
    var type =
        Optional.ofNullable(schema.getType())
            .map(String::toLowerCase)
            .orElse(null);

    return switch (type) {
      case null, "integer", "number", "string", "boolean" -> Stream.of();
      case "object" -> generateObjectAst(currentPackage, schemaName, schema);
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

  private static Stream<PackageContents> generateObjectAst(String currentPackage,
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
      var reference = toReference(interiorPackage, pName, pSchema);
      return new Field(reference, pName);
    }).collect(toList());
    var exteriorClass = Stream.of(new AstClass(schemaName, fields));

    // 2. Define new classes for interior objects (e.g. in-line object definitions for our fields).
    // Note that many properties _may not_ warrant AST generation -- those return an empty stream.
    var interiorClasses =
        properties.entrySet()
            .stream()
            .flatMap(entry -> generateAst(interiorPackage,
                entry.getKey(),
                entry.getValue()));

    return Stream.concat(exteriorClass, interiorClasses);
  }

  private static AstReference toReference(String packageName,
      String schemaName,
      Schema schema) {
    var type = schema.getType();
    var format = schema.getFormat();
    var ref = schema.get$ref();

    return switch (type) {
      case null -> toBasePackageClassReference(ref);
      case "integer", "number", "string", "boolean" -> toStdLibAstReference(type,
          format);
      case "array" -> toListReference(packageName, schemaName, schema);
      case "object" -> new AstReference(packageName, schemaName);
      default -> throw new RuntimeException("Unexpected type: " + type);
    };
  }

  private static AstReference toBasePackageClassReference(String $ref) {
    return new AstReference("",
        $ref.replaceFirst("^#/components/schemas/", ""));
  }

  private static AstReference toStdLibAstReference(String type, String format) {
    return switch (type) {
      case "integer" -> switch (format) {
        case null -> new AstReference("java.math", "BigInteger");
        case "int64" -> new AstReference("java.lang", "Long");
        case "int32" -> new AstReference("java.lang", "Integer");
        default -> throw new IllegalArgumentException(
            "Unexpected integer format: " + format);
      };
      case "number" -> switch (format) {
        case null -> new AstReference("java.math", "BigDecimal");
        case "double" -> new AstReference("java.lang", "Double");
        case "float" -> new AstReference("java.lang", "Float");
        default -> throw new IllegalArgumentException(
            "Unexpected number format: " + format);
      };
      case "string" -> switch (format) {
        case null, "password" -> new AstReference("java.lang", "String");
        case "byte", "binary" -> new AstReference("java.lang", "Byte[]");
        case "date" -> new AstReference("java.time", "LocalDate");
        case "date-time" -> new AstReference("java.time", "ZonedDateTime");
        default -> throw new IllegalArgumentException(
            "Unexpected string format: " + format);
      };
      case "boolean" -> new AstReference("java.lang", "Boolean");
      default -> throw new IllegalArgumentException("Unexpected type: " + type);
    };
  }

  private static AstReference toListReference(String packageName,
      String schemaName,
      Schema schema) {
    var itemSchema = ((ArraySchema) schema).getItems();
    var itemName = schemaName + "Item";
    return new AstReference("java.util",
        "List",
        List.of(toReference(packageName, itemName, itemSchema)));
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