package io.github.tomboyo.lily.compiler.icg;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.AstClassAlias;
import io.github.tomboyo.lily.compiler.ast.Field;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.util.Pair;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Evaluates OAS #/components/schemas into Java AST. */
public class OasComponentsToAst {

  /**
   * Evaluate a component schema (that is, any schema "root" located at #/components/schema),
   * returning a stream of AST.
   *
   * <p>This is similar to {@link OasSchemaToAst}, but adds "aliasing" rules whereby components of
   * scalar, $ref, or array type evaluate to AstClassAlias AST (i.e. type aliases or type wrappers).
   * For example, an array schema named "Boots" whose items are strings and would otherwise evaluate
   * to no AST would instead evaluate to an AstClassAlias describing a class named "Boots" with a
   * single {@code List<String>} field. Typically, a front-end would render this class such that it
   * serializes and deserializes as though it were just a {@code List<String>}, but is otherwise
   * manipulated as a distinct type. This is used to provide semantic domain names to data.
   *
   * @param basePackage Base package under which to generate classes
   * @param componentName The name of the component to evaluate, which may be used to name resulting
   *     classes
   * @param component The schema of the component
   * @return A stream of AST
   */
  public static Stream<Ast> evaluate(
      PackageName basePackage, SimpleName componentName, Schema component) {
    var fqnAndAst = OasSchemaToAst.evaluate(basePackage, componentName, component);

    if (component.get$ref() != null) {
      // Create a AstClassAlias of a referent type. There are no AST elements.
      return Stream.concat(
          Stream.of(
              new AstClassAlias(
                  Fqn.newBuilder().packageName(basePackage).typeName(componentName).build(),
                  fqnAndAst.left())),
          Stream.of());
    }

    if (component instanceof ComposedSchema || component.getNot() != null) {
      return fqnAndAst.right();
    }

    if (component.getType() == null && component.getProperties() != null) {
      return fqnAndAst.right();
    }

    return switch (component.getType()) {
      case "integer", "number", "string", "boolean" -> Stream.concat(
          Stream.of(
              new AstClassAlias(
                  Fqn.newBuilder().packageName(basePackage).typeName(componentName).build(),
                  fqnAndAst.left())),
          fqnAndAst.right());
      case "array" -> evaluateArray(basePackage, fqnAndAst, componentName);
      case "object" -> fqnAndAst.right();
      default -> throw new IllegalArgumentException(
          "Unexpected component type: " + component.getType());
    };
  }

  /**
   * When we create an array alias, we may need to change the package of classes generated from the
   * array's item schema. Normally when evaluating an array schema, if the array's item schema is an
   * object schema, {@link OasSchemaToAst} would generate a class for the item schema in the
   * "current" package (so if the array were a property of an object whose class is "p.MyObject",
   * then the array item class would go in "p.myobject.FieldNameItem.") Therefore, an array
   * component schema would normally put its items in the base package. However, since an alias is a
   * class, we want item classes to be nested "beneath" this component (so if the component class is
   * generated at "p.MyComponent", we want the item to be placed at
   * "p.mycomponent.MyComponentItem.") Thus, we need to update the location of classes and
   * references which evaluate from this array schema definition.
   */
  private static Stream<Ast> evaluateArray(
      PackageName basePackage, Pair<Fqn, Stream<Ast>> fqnAndAst, SimpleName componentName) {
    var ast = fqnAndAst.right().collect(toList());
    // 1. Create a mapping of FQNs that need to move => their updated package names.
    var pattern = Pattern.compile("^" + basePackage);
    var replacement = basePackage.resolve(componentName.toString()).toString();
    var fqnMap =
        ast.stream()
            .filter(it -> it instanceof AstClass)
            .map(Ast::name)
            .collect(
                toMap(
                    identity(),
                    it ->
                        Fqn.newBuilder(it)
                            .packageName(
                                pattern
                                    .matcher(it.packageName().toString())
                                    .replaceFirst(replacement))
                            .build()));

    // 2. Update each such FQN
    var mappedAst =
        ast.stream()
            .map(
                it -> {
                  if (it instanceof AstClass astClass) {
                    return moveClass(astClass, fqnMap);
                  } else {
                    return it;
                  }
                });

    // 3. Create the final AstClassAlias (with updated AstRef!)
    var alias =
        new AstClassAlias(
            Fqn.newBuilder().packageName(basePackage).typeName(componentName).build(),
            moveReference(fqnAndAst.left(), fqnMap));

    return Stream.concat(Stream.of(alias), mappedAst);
  }

  private static AstClass moveClass(AstClass astClass, Map<Fqn, Fqn> nameMap) {
    return AstClass.of(
        nameMap.get(astClass.name()),
        astClass.fields().stream().map(ref -> moveField(ref, nameMap)).collect(toList()));
  }

  private static Field moveField(Field field, Map<Fqn, Fqn> nameMap) {
    return new Field(moveReference(field.astReference(), nameMap), field.name(), field.jsonName());
  }

  private static Fqn moveReference(Fqn ref, Map<Fqn, Fqn> nameMap) {
    return Fqn.newBuilder(nameMap.getOrDefault(ref, ref))
        .typeParameters(
            ref.typeParameters().stream()
                .map(param -> moveReference(param, nameMap))
                .collect(toList()))
        .build();
  }
}
