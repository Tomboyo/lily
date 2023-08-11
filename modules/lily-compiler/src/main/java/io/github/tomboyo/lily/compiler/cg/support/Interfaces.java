package io.github.tomboyo.lily.compiler.cg.support;

import io.github.tomboyo.lily.compiler.ast.Fqn;
import io.github.tomboyo.lily.compiler.ast.HasInterface;
import java.util.stream.Collectors;

public class Interfaces {
  public static String implementsClause(HasInterface hasInterface) {
    if (hasInterface.interfaces().isEmpty()) {
      return "";
    }

    return "implements "
        + hasInterface.interfaces().stream()
            .map(Fqn::toFqpString)
            .collect(Collectors.joining(", "));
  }
}
