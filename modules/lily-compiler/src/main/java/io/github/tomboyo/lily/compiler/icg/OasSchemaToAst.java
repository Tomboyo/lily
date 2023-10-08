package io.github.tomboyo.lily.compiler.icg;

import static java.util.stream.Collectors.toList;

import io.github.tomboyo.lily.compiler.ast.AddInterface;
import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.AstClassAlias;
import io.github.tomboyo.lily.compiler.ast.AstInterface;
import io.github.tomboyo.lily.compiler.ast.Field;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.util.Pair;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
   * Evaluate an OAS schema to AST, returning an Fqn to the top-level type generated from the
   * schema.
   *
   * @param basePackage The root package under which to generate new types.
   * @param name The name of the type to generate.
   * @param schema The schema to evaluate to AST.
   * @return A pair describing the root reference and stream of evaluated AST.
   */
  public static Pair<Fqn, Stream<Ast>> evaluate(
      PackageName basePackage, SimpleName name, Schema<?> schema) {
    return new OasSchemaToAst(basePackage).evaluateSchema(basePackage, name, schema);
  }

  /**
   * Evaluate an OAS schema to AST, returning an Fqn to the top-level type generated from the
   * schema.
   *
   * @param basePackage The package containing all "top level" types ever generated.
   * @param genRoot The package to contain all types generated for this schema or nested packages of
   *     generated types.
   * @param name The name of the type to generate.
   * @param schema The schema to evaluate to AST.
   * @return A pair describing the root reference and stream of evaluated AST.
   */
  public static Pair<Fqn, Stream<Ast>> evaluateInto(
      PackageName basePackage, PackageName genRoot, SimpleName name, Schema<?> schema) {
    return new OasSchemaToAst(basePackage).evaluateSchema(genRoot, name, schema);
  }

  /**
   * Return the FQN of a shared component with the given $ref name. If and when the component is
   * generated, the Fqn will point to the generated type.
   *
   * @param basePackage The package containing all "top level" generated types.
   * @param ref The name of the shared component, like '#/components/schemas'
   * @return The Fqn.
   */
  public static Fqn fqnForRef(PackageName basePackage, String ref) {
    return new OasSchemaToAst(basePackage).toBasePackageClassReference(ref);
  }

  private Pair<Fqn, Stream<Ast>> evaluateSchema(
      PackageName currentPackage, SimpleName name, Schema<?> schema) {

    if (schema.getNot() != null) {
      LOGGER.warn("The `not` keyword is not yet supported.");
    }

    var type = schema.getType();
    if (type == null) {
      if (schema.getProperties() != null || schema instanceof ComposedSchema) {
        return evaluateObject(currentPackage, name, schema);
      } else if (schema instanceof ArraySchema a) {
        return evaluateArray(currentPackage, name, a);
      } else if (schema.get$ref() != null) {
        return new Pair<>(toBasePackageClassReference(schema.get$ref()), Stream.of());
      } else {
        throw new RuntimeException("Can not determine type for schema " + name);
      }
    }

    return switch (type) {
      case "integer", "number", "string", "boolean" -> new Pair<>(
          toStdLibFqn(schema.getType(), schema.getFormat()), Stream.of());
      case "array" -> evaluateArray(currentPackage, name, (ArraySchema) schema);
      case "object" -> evaluateObject(currentPackage, name, schema);
      default -> throw new IllegalArgumentException(("Unexpected type: " + type));
    };
  }

  private Fqn toBasePackageClassReference(String $ref) {
    return Fqn.newBuilder()
        .packageName(basePackage)
        .typeName($ref.replaceFirst("^#/components/schemas/", ""))
        .build();
  }

  private Fqn toStdLibFqn(String type, String format) {
    switch (type) {
      case "integer":
        if (format == null) {
          return StdlibFqns.astBigInteger();
        }

        return switch (format) {
          case "int64" -> StdlibFqns.astLong();
          case "int32" -> StdlibFqns.astInteger();
          default -> defaultForUnsupportedFormat(type, format, StdlibFqns.astBigInteger());
        };
      case "number":
        if (format == null) {
          return StdlibFqns.astBigDecimal();
        }

        return switch (format) {
          case "double" -> StdlibFqns.astDouble();
          case "float" -> StdlibFqns.astFloat();
          default -> defaultForUnsupportedFormat(type, format, StdlibFqns.astBigDecimal());
        };
      case "string":
        if (format == null) {
          return StdlibFqns.astString();
        }

        return switch (format) {
          case "password" -> StdlibFqns.astString();
          case "byte", "binary" -> StdlibFqns.astByteBuffer();
          case "date" -> StdlibFqns.astLocalDate();
          case "date-time" -> StdlibFqns.astOffsetDateTime();
          default -> defaultForUnsupportedFormat(type, format, StdlibFqns.astString());
        };
      case "boolean":
        if (format == null) {
          return StdlibFqns.astBoolean();
        } else {
          return defaultForUnsupportedFormat(type, format, StdlibFqns.astBoolean());
        }
      default:
        throw new IllegalArgumentException("Unexpected type: " + type);
    }
  }

  private Fqn defaultForUnsupportedFormat(String type, String format, Fqn defaultAst) {
    if (format != null) {
      LOGGER.warn("Using default class for unsupported format: type={} format={}", type, format);
    }
    return defaultAst;
  }

  private Fqn fqnForOneOfAlias(
      PackageName currentPackage, SimpleName oneOfName, String type, String format) {
    var simpleName = oneOfName.resolve(type);
    if (format != null) {
      simpleName = simpleName.resolve(format);
    }

    return Fqn.newBuilder(currentPackage, simpleName.resolve("Alias")).build();
  }

  private boolean isPrimitiveType(String type) {
    return type != null && List.of("integer", "number", "string", "boolean").contains(type);
  }

  private Pair<Fqn, Stream<Ast>> evaluateArray(
      PackageName currentPackage, SimpleName name, ArraySchema arraySchema) {
    if ("object".equals(arraySchema.getItems().getType())
        || (arraySchema.getItems().getType() == null
            && arraySchema.getItems().getProperties() != null)) {
      /*
       AST generated from objects in arrays are named similarly to all other objects in that they
       are defined within subordinate packages of superior types (if any). They are also given the "Item" suffix. For
       example:

       1. Suppose the #/components/Cats component is an array containing [arrays of] objects. If
       the base package is p, then the resulting Fqn should be:

          List<[List<...<]p.CatsItem[>...>]>

       2. Suppose the #/components/Bus component is an object with a "wheels" array parameter
       which contains [arrays of] objects. Then the resulting Fqn should be:

          List<[List<...<]p.bus.WheelsItem[>...>]>

       Regardless of how many arrays are nested within an array, the Item suffix as appended only
       once to the referent type name. I.e. we do not generate WheelsItemItem...Item. Furthermore, since arrays do not
       generate their own
       types (for exceptions, see OasComponentsToAst), nested arrays do not affect the package
       name of the referent
       type.
      */
      var interior = evaluateSchema(currentPackage, name.resolve("Item"), arraySchema.getItems());
      return new Pair<>(StdlibFqns.astListOf(interior.left()), interior.right());
    } else {
      // All types other than Object do not result in new AST, so we do not use the naming strategy
      // used for objects.
      var interior = evaluateSchema(currentPackage, name, arraySchema.getItems());
      return new Pair<>(StdlibFqns.astListOf(interior.left()), interior.right());
    }
  }

  private Pair<Fqn, Stream<Ast>> evaluateObject(
      PackageName currentPackage, SimpleName name, Schema<?> schema) {
    var properties = getProperties(schema);
    var requiredPropertyNames = getRequiredPropertyNames(schema);
    var interiorPackage = currentPackage.resolve(name.toString());

    /*
     Generate the AST required to define the fields of this new class. If we define any classes
     for our fields, we place them in a subordinate package named after this class. For example,
     if we define the class package.MyClass, then any classes defined for MyClass' fields are
     defined under the package.myclass package.
    */
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
                  var isMandatory =
                      (fieldSchema.getNullable() == null || !fieldSchema.getNullable())
                          && requiredPropertyNames.contains(jsonName);

                  return refAndAst.mapLeft(
                      ref -> new Field(ref, SimpleName.of(jsonName), jsonName, isMandatory));
                })
            .toList();

    var exteriorClass =
        AstClass.of(
            Fqn.newBuilder().packageName(currentPackage).typeName(name).build(),
            fieldAndAst.stream().map(Pair::left).collect(toList()));
    var interiorAst = fieldAndAst.stream().flatMap(Pair::right);
    return new Pair<>(
        Fqn.newBuilder().packageName(currentPackage).typeName(name).build(),
        Stream.concat(Stream.of(exteriorClass), interiorAst));
  }

  /* Gets properties from both properties keywords and composed schema */
  @SuppressWarnings({"rawtypes", "unchecked"}) // not shit I can do about the io.swagger API
  private Map<String, Schema> getProperties(Schema schema) {
    var properties = new HashMap<String, Schema>();
    if (schema.getProperties() != null) {
      properties.putAll(schema.getProperties());
    }

    if (schema instanceof ComposedSchema c) {
      Stream.of(c.getAllOf(), c.getAnyOf(), c.getOneOf())
          .filter(Objects::nonNull)
          .flatMap(List::stream)
          .forEach(s -> properties.putAll(getProperties(s)));
    }

    return properties;
  }

  private Set<String> getRequiredPropertyNames(Schema<?> root) {
    var set = new HashSet<String>();
    if (root.getRequired() != null) {
      set.addAll(root.getRequired());
    }

    if (root instanceof ComposedSchema c) {
      Stream.ofNullable(c.getAllOf())
          .flatMap(List::stream)
          .flatMap(schema -> getRequiredPropertyNames((Schema<?>) schema).stream())
          .forEach(set::add);
    }

    return set;
  }

  /*
   * OneOf composed schemas evaluate to a sealed interface. The schema may
   * be composed of new or existing schemas; in either case, we emit
   * AddInterface modifiers so that those AST are updated to
   * implement the new sealed interface before rendering.
   */
  private Pair<Fqn, Stream<Ast>> evaluateOneOf(
      ComposedSchema schema, PackageName currentPackage, SimpleName name) {
    var ifaceName = Fqn.newBuilder().packageName(currentPackage).typeName(name).build();

    var counter = new AtomicInteger(1);
    var pairs =
        schema.getOneOf().stream()
            .map(
                s -> {
                  // If s is a primitive type, we'll create an alias called e.g.
                  // com.example.MySchemaStringEmailAlias.
                  if (isPrimitiveType(s.getType())) {
                    var fqn = fqnForOneOfAlias(currentPackage, name, s.getType(), s.getFormat());
                    return new Pair<>(
                        fqn,
                        Stream.of(
                            AstClassAlias.aliasOf(
                                fqn, toStdLibFqn(s.getType(), s.getFormat()), List.of(ifaceName))));
                  } else {
                    // Otherwise, we'll name the next type e.g. com.example.MySchema1. This will
                    // get used only if we end up generating an inline object schema.
                    var intermediate =
                        evaluateSchema(
                            currentPackage,
                            name.resolve(Integer.toString(counter.getAndIncrement())),
                            s);
                    return intermediate.mapRight(
                        stream ->
                            Stream.concat(
                                stream,
                                Stream.of(new AddInterface(intermediate.left(), ifaceName))));
                  }
                })
            .toList();

    var permits = pairs.stream().map(Pair::left).toList();
    var iface = new AstInterface(ifaceName, permits);
    return new Pair<>(
        ifaceName, Stream.concat(Stream.of(iface), pairs.stream().flatMap(Pair::right)));
  }
}
