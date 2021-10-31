package com.github.tomboyo.lily;

import com.github.tomboyo.lily.ast.type.*;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.media.Schema;

import java.util.Map;
import java.util.stream.Collectors;

public class Main {
  public static void main(String[] args) {
    var specPath = "petstore.yaml";
    var parseResult = new OpenAPIParser().readLocation(specPath, null, null);

    var errors = parseResult.getMessages();
    errors.forEach(System.err::println);

    var openApi = parseResult.getOpenAPI();
    if (openApi == null) return;
    var version = openApi.getOpenapi();
    if (version == null || !version.startsWith("3.")) {
      System.err.println("OAS specification version is not 3.x: version=" + version);
    } else {
      System.out.println("OAS version: version=" + version);
    }

    var componentAst =
        openApi.getComponents().getSchemas().entrySet().stream()
            .map(entry -> generateRootComponentAst(entry.getKey(), entry.getValue()))
            .collect(Collectors.toSet());
    componentAst.forEach(System.out::println);
  }

  private static Type generateRootComponentAst(String componentName, Schema componentSchema) {
    var type = componentSchema.getType();
    if (type == null)
      throw new IllegalArgumentException(
          "Root component has no type: componentName=" + componentName);

    switch (type) {
      case "object":
        return generateOuterClassFromComponent(componentName, componentSchema);
      default:
        throw new IllegalArgumentException(
            "Unsupported root component type: componentName="
                + componentName
                + " componentType="
                + type);
    }
  }

  private static OuterClass generateOuterClassFromComponent(
      String componentName, Schema componentSchema) {
    if (!componentSchema.getType().equalsIgnoreCase("object")) throw new IllegalArgumentException();

    return new OuterClass(
        componentName,
        ((Map<String, Schema>) componentSchema.getProperties())
            .entrySet().stream()
                .map(entry -> oasObjectPropertyToField(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
  }

  private static Field oasObjectPropertyToField(String propertyName, Schema propertySchema) {
    var type = propertySchema.getType();
    var ref = propertySchema.get$ref();

    if (type != null) {
      var format = propertySchema.getFormat();
      return new Field(oasObjectPropertyType(propertyName, type, format), propertyName);
    } else if (ref != null) {
      // TODO
      return new Field(new UnsupportedType("$ref"), propertyName);
    } else {
      throw new IllegalArgumentException("Missing type or $ref: propertyName=" + propertyName);
    }
  }

  // See https://swagger.io/specification/#data-types
  // Note: no primitive java types. All fields from an API are inherently nullable, even if this
  // breaks the APIs contract, because the user may request a partial (fragment, filtered, etc)
  // response using query parameters or similar.
  // TODO: unexpected format should be warnings only -- and potential extension points to support
  // user-defined formats like "email," as per the spec linked above.
  private static Type oasObjectPropertyType(String propertyName, String type, String format) {
    switch (type) {
      case "integer":
        if (format == null) return new StandardType("java.math.BigInteger");
        if (format.equalsIgnoreCase("int64")) return new StandardType("Long");
        if (format.equalsIgnoreCase("int32")) return new StandardType("Integer");
        throw new IllegalArgumentException("Unexpected integer format: " + format);
      case "number":
        if (format == null) return new StandardType("java.math.BigDecimal");
        if (format.equalsIgnoreCase("double")) return new StandardType("Double");
        if (format.equalsIgnoreCase("float")) return new StandardType("Float");
        throw new IllegalArgumentException("Unexpected number format: " + format);
      case "string":
        if (format == null || format.equalsIgnoreCase("password"))
          return new StandardType("String");
        // base64 or octets.
        if (format.equalsIgnoreCase("byte") || format.equalsIgnoreCase("binary"))
          return new StandardType("Byte[]");
        if (format.equalsIgnoreCase("date")) return new StandardType("java.time.LocalDate");
        if (format.equalsIgnoreCase("date-time"))
          return new StandardType("java.time.ZonedDateTime");
        throw new IllegalArgumentException("Unexpected string format: " + format);
      case "boolean":
        return new StandardType("Boolean");
      case "object":
        return new InnerClass(propertyName);
      case "array":
        return new UnsupportedType("inline array type");
      default:
        return new UnsupportedType("type=" + type + " format=" + format);
    }
  }
}
