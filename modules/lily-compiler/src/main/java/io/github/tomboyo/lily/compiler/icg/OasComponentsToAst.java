package io.github.tomboyo.lily.compiler.icg;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.AstClassAlias;
import io.github.tomboyo.lily.compiler.ast.AstField;
import io.github.tomboyo.lily.compiler.ast.AstReference;
import io.github.tomboyo.lily.compiler.cg.Fqns;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Collection;
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
  public static Stream<Ast> evaluate(String basePackage, String componentName, Schema component) {
    var refAndAst = OasSchemaToAst.evaluate(basePackage, componentName, component);

    if (null == component.getType()) {
      // Create a AstClassAlias of a referent type. There are no AST elements.
      return Stream.concat(
          Stream.of(new AstClassAlias(basePackage, componentName, refAndAst.left())),
          refAndAst.right());
    }

    return switch (component.getType()) {
        // Create a AstClassAlias of a scalar type. There are no AST elements.
      case "integer", "number", "string", "boolean" -> Stream.concat(
          Stream.of(new AstClassAlias(basePackage, componentName, refAndAst.left())),
          refAndAst.right());
        // ToDo: signature of moveClasses is at the "it works and I am exhausted" stage.
        // Rewrite.
      case "array" -> moveClasses(
          refAndAst.right().collect(toList()),
          refAndAst.left(),
          componentName,
          basePackage,
          Support.joinPackages(basePackage, componentName));
      case "object" -> refAndAst.right(); // No aliasing required for new types
      default -> throw new IllegalArgumentException(
          "Unexpected component type: " + component.getType());
    };
  }

  /**
   * Move all new classes and references to those (and only those) classes to a new package by
   * replacing the from package prefix with the to package prefix.
   *
   * <p>1: Identify the FQN of all generated classes, and use this to create a mapping of new
   * package names. 2: Update all AstReferences pointing at any of the names from 1. Leave all
   * others unchanged so that $refs to top- level components or provided types evaluate as normal.
   *
   * <p>The only classes we need to move are for subordinate object schemas used to create the
   * model. All AstReferences to these classes must themselves also be in the model, so we only need
   * to search AstClasses themselves for references that need to migrate.
   */
  private static Stream<Ast> moveClasses(
      Collection<Ast> ast, AstReference ref, String newName, String from, String to) {
    var pattern = Pattern.compile("^" + from);

    // 1. Create mapping of fullyQualifiedName => newPackage
    var mapping =
        ast.stream()
            .filter(it -> it instanceof AstClass)
            .map(it -> (AstClass) it)
            .collect(toMap(Fqns::fqn, it -> pattern.matcher(it.packageName()).replaceFirst(to)));

    // 2. Update AstReferences to any FQN in step 1 ONLY.
    var mappedAst =
        ast.stream()
            .map(
                it -> {
                  if (it instanceof AstClass astClass) {
                    return moveClass(astClass, mapping);
                  } else {
                    return it;
                  }
                });

    // 3. Create the AstClassAlias (with updated AstRef!)
    var alias = new AstClassAlias(from, newName, moveReference(ref, mapping));

    return Stream.concat(Stream.of(alias), mappedAst);
  }

  private static AstClass moveClass(AstClass astClass, Map<String, String> mapping) {
    return AstClass.of(
        // All classes in this AST stream have to move, so we know this mapping is defined.
        mapping.get(Fqns.fqn(astClass)),
        astClass.name(),
        astClass.fields().stream().map(ref -> moveField(ref, mapping)).collect(toList()));
  }

  private static AstField moveField(AstField field, Map<String, String> mapping) {
    var key = Fqns.fqn(field.astReference());
    return new AstField(moveReference(field.astReference(), mapping), field.name());
  }

  /**
   * Bear in mind that the type parameters may need updating even if the outermost referents do not.
   */
  private static AstReference moveReference(AstReference ref, Map<String, String> mapping) {
    var key = Fqns.fqn(ref);
    return new AstReference(
        mapping.getOrDefault(key, ref.packageName()),
        ref.name(),
        ref.typeParameters().stream().map(param -> moveReference(param, mapping)).collect(toList()),
        ref.isProvidedType());
  }
}
