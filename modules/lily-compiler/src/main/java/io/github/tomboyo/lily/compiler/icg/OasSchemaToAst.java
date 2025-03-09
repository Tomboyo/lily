package io.github.tomboyo.lily.compiler.icg;

import static java.util.stream.Collectors.toList;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.Field;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.oas.model.ISchema;
import io.github.tomboyo.lily.compiler.oas.model.None;
import io.github.tomboyo.lily.compiler.oas.model.Ref;
import io.github.tomboyo.lily.compiler.oas.model.Schema;
import io.github.tomboyo.lily.compiler.util.Pair;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
   * @param iSchema The schema to evaluate to AST.
   * @return A pair describing the root reference and stream of evaluated AST.
   */
  public static Pair<Fqn, Stream<Ast>> evaluate(
      PackageName basePackage, SimpleName name, ISchema iSchema) {
    return new OasSchemaToAst(basePackage).evaluateSchema(basePackage, name, iSchema);
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
      PackageName basePackage, PackageName genRoot, SimpleName name, ISchema schema) {
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
      PackageName currentPackage, SimpleName name, ISchema iSchema) {
    return switch (iSchema) {
      case None none -> throw new RuntimeException("TODO");
      case Ref(String $ref) -> new Pair<>(toBasePackageClassReference($ref), Stream.of());
      case Schema schema when schema.isObject() -> evaluateObject(currentPackage, name, schema);
      case Schema schema when schema.isArray() -> evaluateArray(currentPackage, name, schema);
      case Schema schema -> {
        if (schema.type().isEmpty()) {
          // TODO: skip this schema instead of exploding
          throw new RuntimeException("Can not determine type for schema " + name);
        }

        var type = schema.type().get();
        yield switch (type) {
          case "integer", "number", "string", "boolean" ->
              new Pair<>(toStdLibFqn(type, schema.format()), Stream.of());
          case "array" -> evaluateArray(currentPackage, name, schema);
          case "object" -> evaluateObject(currentPackage, name, schema);
          default -> throw new IllegalArgumentException(("Unexpected type: " + type));
        };
      }
    };
  }

  private Fqn toBasePackageClassReference(String $ref) {
    return Fqn.newBuilder()
        .packageName(basePackage)
        .typeName($ref.replaceFirst("^#/components/schemas/", ""))
        .build();
  }

  private Fqn toStdLibFqn(String type, Optional<String> format) {
    switch (type) {
      case "integer":
        return switch (format.orElse("null")) {
          case "null" -> StdlibFqns.astBigInteger();
          case "int64" -> StdlibFqns.astLong();
          case "int32" -> StdlibFqns.astInteger();
          default -> defaultForUnsupportedFormat(type, format, StdlibFqns.astBigInteger());
        };
      case "number":
        return switch (format.orElse("null")) {
          case "null" -> StdlibFqns.astBigDecimal();
          case "double" -> StdlibFqns.astDouble();
          case "float" -> StdlibFqns.astFloat();
          default -> defaultForUnsupportedFormat(type, format, StdlibFqns.astBigDecimal());
        };
      case "string":
        return switch (format.orElse("null")) {
          case "null" -> StdlibFqns.astString();
          case "password" -> StdlibFqns.astString();
          case "byte", "binary" -> StdlibFqns.astByteBuffer();
          case "date" -> StdlibFqns.astLocalDate();
          case "date-time" -> StdlibFqns.astOffsetDateTime();
          default -> defaultForUnsupportedFormat(type, format, StdlibFqns.astString());
        };
      case "boolean":
        if (format.isEmpty()) {
          return StdlibFqns.astBoolean();
        } else {
          return defaultForUnsupportedFormat(type, format, StdlibFqns.astBoolean());
        }
      default:
        throw new IllegalArgumentException("Unexpected type: " + type);
    }
  }

  private Fqn defaultForUnsupportedFormat(String type, Optional<String> format, Fqn defaultAst) {
    if (format.isPresent()) {
      LOGGER.warn(
          "Using default class for unsupported format: type={} format={}", type, format.get());
    }
    return defaultAst;
  }

  private Pair<Fqn, Stream<Ast>> evaluateArray(
      PackageName currentPackage, SimpleName name, Schema schema) {
    var iSchema = schema.items();
    return switch (schema.items()) {
      // TODO: correct None handling
      case None none -> throw new RuntimeException("Unimplemented None branch");
      case Schema items when items.isObject() -> {
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
        var interior = evaluateSchema(currentPackage, name.resolve("Item"), items);
        yield new Pair<>(StdlibFqns.astListOf(interior.left()), interior.right());
      }
      default -> {
        /* Refs and non-object schemas do not result in new AST, so we do not use the naming strategy used for objects. */
        var interior = evaluateSchema(currentPackage, name, iSchema);
        yield new Pair<>(StdlibFqns.astListOf(interior.left()), interior.right());
      }
    };
  }

  private Pair<Fqn, Stream<Ast>> evaluateObject(
      PackageName currentPackage, SimpleName name, Schema schema) {
    var properties = getProperties(schema);
    var mandatoryPropertyNames = getMandatoryPropertyNames(schema);
    var interiorPackage = currentPackage.resolve(name.toString());

    /*
     Generate the AST required to define the fields of this new class. If we define any classes
     for our fields, we place them in a subordinate package named after this class. For example,
     if we define the class package.MyClass, then any classes defined for MyClass' fields are
     defined under the package.myclass package.
    */
    var fieldAndAst =
        properties.entrySet().stream()
            .filter(entry -> entry.getValue() != None.NONE)
            .map(
                entry -> {
                  var jsonName = entry.getKey();
                  var fieldSchema = entry.getValue();
                  var fieldPackage =
                      switch (fieldSchema) {
                        case None none ->
                            throw new RuntimeException("Unexpected None"); // already filtered out
                        case Ref ref -> basePackage; // $ref's always point to a base package type.
                        case Schema s -> interiorPackage;
                      };

                  var refAndAst =
                      evaluateSchema(fieldPackage, SimpleName.of(jsonName), fieldSchema);
                  var isMandatory = mandatoryPropertyNames.contains(jsonName);

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
  private Map<String, ISchema> getProperties(ISchema iSchema) {
    // TODO: is this correct handling of $ref?
    if (iSchema instanceof Schema schema) {
      var properties = new HashMap<>(schema.properties());

      if (schema.isComposed()) {
        Stream.of(schema.allOf(), schema.anyOf(), schema.oneOf())
            .flatMap(List::stream)
            .forEach(s -> properties.putAll(getProperties(s)));
      }

      return properties;
    } else {
      return Map.of();
    }
  }

  private Set<String> getMandatoryPropertyNames(ISchema iSchema) {
    if (iSchema instanceof Schema schema) {
      var required = requiredPropertyNames(schema);
      var nonNullable = getNonNullablePropertyNames(schema);
      var mandatory = intersection(required, nonNullable);

      if (schema.isComposed()) {
        /* If all the oneOf schema (one of which MUST validate) agree that a given property is mandatory, then it is
        mandatory on the composed schema as well. */
        schema.oneOf().stream()
            .map(this::getMandatoryPropertyNames)
            .reduce(this::intersection)
            .ifPresent(mandatory::addAll);

        /* If a property is mandatory according to an allOf component, then it is mandatory according to the composed
        schema as well. */
        schema.allOf().stream().map(this::getMandatoryPropertyNames).forEach(mandatory::addAll);
      }

      return mandatory;
    } else {
      return Set.of();
    }
  }

  private Set<String> requiredPropertyNames(ISchema iSchema) {
    // TODO: review $ref/None handling
    if (iSchema instanceof Schema schema) {
      var required = new HashSet<>(schema.required());

      if (schema.isComposed()) {
        /* AllOf component required keywords are flattened into the composed schema. */
        schema.allOf().stream()
            .map(this::requiredPropertyNames)
            .flatMap(Set::stream)
            .forEach(required::add);

        /* When OneOf components "have consensus," i.e. they agree that a property is required, then that requirement is
        propagated to the composed schema as well. (One OneOf _must_ validate, so if they all say a property is required,
        then it's required.) */
        schema.oneOf().stream()
            .map(this::requiredPropertyNames)
            .reduce(this::intersection)
            .ifPresent(required::addAll);
      }

      return required;
    } else {
      return Set.of();
    }
  }

  private Set<String> getNonNullablePropertyNames(ISchema iSchema) {
    // TODO: properly handle $refs, where the nullable property is defined on the referent
    if (iSchema instanceof Schema schema) {
      var result = new HashSet<String>();

      schema.properties().entrySet().stream()
          // TODO: properly handle $refs, where the nullable property is defined on the referent
          .filter(entry -> entry.getValue() instanceof Schema s && !s.nullable().orElse(false))
          .map(Map.Entry::getKey)
          .forEach(result::add);

      if (schema.isComposed()) {
        // When an allOf property is not nullable, then it's not nullable on the composed schema
        // either
        schema.allOf().stream()
            .map(this::getNonNullablePropertyNames)
            .flatMap(Set::stream)
            .forEach(result::add);

        /* If every oneOf schema agrees that a property is not nullable ("consensus"), then it's not nullable on the
        composed schema either. */
        schema.oneOf().stream()
            .map(this::getNonNullablePropertyNames)
            .reduce(this::intersection)
            .ifPresent(result::addAll);
      }

      // AnyOf schema are inherently optional, so their properties are irrelevant.

      return result;
    } else {
      return Set.of();
    }
  }

  private <T> HashSet<T> intersection(Collection<T> a, Collection<T> b) {
    var result = new HashSet<>(a);
    result.retainAll(b);
    return result;
  }
}
