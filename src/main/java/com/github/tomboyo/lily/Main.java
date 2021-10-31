package com.github.tomboyo.lily;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.media.Schema;

import java.util.Map;

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

    var schemas = openApi.getComponents().getSchemas();
    generateSchemas(schemas);
  }

  private static void generateSchemas(Map<String, Schema> schemas) {
    schemas.forEach(Main::generateModel);
  }

  private static void generateModel(String name, Schema schema) {
    var type = schema.getType();
    if (type == null) {
      println("Schema " + name + " has null type");
      return;
    }

    switch (type) {
      case "object":
        generateObjectModel(name, schema);
        return;
      default:
        throw new RuntimeException("Type of top-level model '" + name + "' is null");
    }
  }

  private static void generateObjectModel(String name, Schema schema) {
    var className = oasSchemaNameToJavaClassName(name);
    Map<String, Schema> properties = schema.getProperties();
    println("public class " + className + " {");
    properties.forEach(Main::generateJavaObjectFieldFromOasProprty);
    println("}");
  }

  // TODO: convert the given schema name to a class name
  private static String oasSchemaNameToJavaClassName(String schemaName) {
    return schemaName.replaceFirst("^#/components/schemas/", "");
  }

  private static void generateJavaObjectFieldFromOasProprty(
      String propertyName, Schema propertySchema) {
    var type = oasPropertyToJavaType(propertySchema);
    var fieldName = getFieldName(propertyName);
    println("  private " + type + " " + fieldName + ";");
  }

  // See https://swagger.io/specification/#data-types
  // Note: no primitive java types. All fields from an API are inherently nullable, even if this
  // breaks the APIs contract, because the user may request a partial (fragment, filtered, etc)
  // response using query parameters or similar.
  // TODO: unexpected format should be warnings only -- and potential extension points to support
  // user-defined formats like "email," as per the spec linked above.
  private static String oasPropertyToJavaType(Schema propertySchema) {
    var type = propertySchema.getType();
    if (type != null) {
      var format = propertySchema.getFormat();

      switch (type) {
        case "integer":
          if (format == null) return "java.math.BigInteger";
          if (format.equalsIgnoreCase("int64")) return "Long";
          if (format.equalsIgnoreCase("int32")) return "Integer";
          throw new IllegalArgumentException("Unexpected integer format: " + format);
        case "number":
          if (format == null) return "java.math.BigDecimal";
          if (format.equalsIgnoreCase("double")) return "Double";
          if (format.equalsIgnoreCase("float")) return "Float";
          throw new IllegalArgumentException("Unexpected number format: " + format);
        case "string":
          if (format == null || format.equalsIgnoreCase("password")) return "String";
          // base64 or octets.
          if (format.equalsIgnoreCase("byte") || format.equalsIgnoreCase("binary")) return "Byte[]";
          if (format.equalsIgnoreCase("date")) return "java.time.LocalDate";
          if (format.equalsIgnoreCase("date-time")) return "java.time.ZonedDateTime";
          throw new IllegalArgumentException("Unexpected string format: " + format);
        case "boolean":
          return "Boolean";
        case "object":
          return "TODO: INLINE OBJECT";
        case "array":
          return "TODO: INLINE ARRAY";
        default:
          throw new IllegalArgumentException("Unexpected type: " + type);
      }
    } else if (propertySchema.get$ref() != null) {
      return oasSchemaNameToJavaClassName(propertySchema.get$ref());
    } else {
      return "<UNKNOWN JAVA TYPE>";
    }
  }

  // TODO: escapse and potentially format the given string as a java object field name
  private static String getFieldName(String name) {
    return name;
  }

  private static void println(String m) {
    System.out.println(m);
  }
}
