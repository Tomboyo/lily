package io.github.tomboyo.lily.compiler.ast;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * A type alias, or alternative name, for some other type.
 *
 * <p>There is no first-class Java representation of a type alias, but it could be rendered either
 * by substituting for the referent type, or as a "wrapper" class.
 */
public record AstClassAlias(Fqn name, Fqn aliasedType, LinkedHashSet<Fqn> interfaces)
    implements Definition, HasInterface {

  public static AstClassAlias aliasOf(Fqn name, Fqn aliasedType) {
    return new AstClassAlias(name, aliasedType, new LinkedHashSet<>());
  }

  public static AstClassAlias aliasOf(Fqn name, Fqn aliasedType, List<Fqn> interfaces) {
    return new AstClassAlias(name, aliasedType, new LinkedHashSet<>(interfaces));
  }

  @Override
  public AstClassAlias addInterface(Fqn interfaceName) {
    return new AstClassAlias(name, aliasedType, add(interfaces, interfaceName));
  }

  private static <T> LinkedHashSet<T> add(LinkedHashSet<T> set, T el) {
    var tmp = new LinkedHashSet<>(set);
    tmp.add(el);
    return tmp;
  }
}
