package com.github.tomboyo.lily.ast.type;

public sealed interface Ast permits StdLibType, NewClass, ClassRef, UnsupportedAst {}
