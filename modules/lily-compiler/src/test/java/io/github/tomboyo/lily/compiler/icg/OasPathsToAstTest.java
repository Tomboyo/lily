package io.github.tomboyo.lily.compiler.icg;

import static io.github.tomboyo.lily.compiler.AstSupport.astReferencePlaceholder;
import static io.swagger.v3.oas.models.PathItem.HttpMethod.PUT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

import io.github.tomboyo.lily.compiler.ast.AstOperation;
import io.github.tomboyo.lily.compiler.ast.AstTaggedOperations;
import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.PackageName;
import io.github.tomboyo.lily.compiler.ast.SimpleName;
import io.github.tomboyo.lily.compiler.icg.OasOperationToAst.TagsOperationAndAst;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class OasPathsToAstTest {

  @Nested
  class EvaluatePathItem {

    @Test
    void evaluatesAllOperations() {
      try (var mock = mockStatic(OasOperationToAst.class)) {
        OasPathsToAst.evaluatePathItem(
                PackageName.of("p"),
                "/operation/path",
                new PathItem()
                    .addParametersItem(new Parameter().name("name").in("path"))
                    .get(new Operation())
                    .put(new Operation())
                    .post(new Operation())
                    .patch(new Operation())
                    .delete(new Operation())
                    .head(new Operation())
                    .options(new Operation()))
            .forEach(x -> {}); // consume the stream

        // It should evaluate each operation in turn, passing down the base package, operation
        // relative path, and inherited parameters from the PathItem.
        mock.verify(
            () ->
                OasOperationToAst.evaluateOperaton(
                    eq(PackageName.of("p")),
                    eq("/operation/path"),
                    any(),
                    any(),
                    eq(List.of(new Parameter().name("name").in("path")))),
            times(7));
      }
    }

    @Test
    void evaluatesOperationsWithHttpMethod() {
      try (var mock = mockStatic(OasOperationToAst.class)) {
        OasPathsToAst.evaluatePathItem(
                PackageName.of("p"),
                "/operation/path",
                new PathItem()
                    .addParametersItem(new Parameter().name("name").in("path"))
                    .put(new Operation()))
            .forEach(x -> {}); // consume the stream

        mock.verify(
            () ->
                OasOperationToAst.evaluateOperaton(
                    eq(PackageName.of("p")),
                    eq("/operation/path"),
                    eq(PUT),
                    any(),
                    eq(List.of(new Parameter().name("name").in("path")))));
      }
    }
  }

  @Nested
  class EvaluateTaggedOperations {

    @Test
    void groupsOperationsByTag() {
      var getAOperation =
          new AstOperation(
              SimpleName.of("GetA"), astReferencePlaceholder(), "GET", "getA/", List.of());
      var getABOperation =
          new AstOperation(
              SimpleName.of("GetAB"), astReferencePlaceholder(), "GET", "getAB/", List.of());
      var actual =
          OasPathsToAst.evaluateTaggedOperations(
                  PackageName.of("p"),
                  List.of(
                      new TagsOperationAndAst(Set.of("TagA"), getAOperation, Set.of()),
                      new TagsOperationAndAst(Set.of("TagA", "TagB"), getABOperation, Set.of())))
              .collect(Collectors.toSet());

      assertThat(
          "Tagged operations AST are collections of operations grouped by tag",
          actual,
          is(
              Set.of(
                  new AstTaggedOperations(
                      Fqn.of("p", "TagAOperations"), Set.of(getAOperation, getABOperation)),
                  new AstTaggedOperations(Fqn.of("p", "TagBOperations"), Set.of(getABOperation)))));
    }
  }
}
