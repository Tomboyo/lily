package com.github.tomboyo.lily.ast.type;

import java.util.List;

public record AstClass(String name, List<Field> fields) implements Type {}
