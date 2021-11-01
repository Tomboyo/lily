package com.github.tomboyo.lily.ast.type;

import java.util.List;

public record StandardType(String fqn, List<Type> typeParameters) implements Type {
    public StandardType(String fqn) {
        this(fqn, List.of());
    }
}
