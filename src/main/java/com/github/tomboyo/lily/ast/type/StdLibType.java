package com.github.tomboyo.lily.ast.type;

import java.util.List;

public record StdLibType(String fqn, List<Ast> typeParameters) implements Ast {
    public StdLibType(String fqn) {
        this(fqn, List.of());
    }
}
