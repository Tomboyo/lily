package io.github.tomboyo.lily.compiler.icg;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class OasPathsToAstTest {

  @Nested
  class PathItemParameters {
    @ParameterizedTest
    @CsvSource({"boolean", "integer", "number", "string"})
    void generateNoAstForScalarTypes(String type) {
      var actual =
          OasPathsToAst.evaluatePathItem(
              "p",
              new PathItem()
                  .parameters(
                      List.of(
                          new Parameter()
                              .name("myparam")
                              .in("path")
                              .schema(new Schema().type(type)))));

      assertThat(actual.collect(Collectors.toSet()), empty());
    }
  }
}
