package com.github.tomboyo.lily.ast.type;

import java.util.List;

public record NewClass(String name, List<Field> fields) implements Ast {}
