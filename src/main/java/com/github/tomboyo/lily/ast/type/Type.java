package com.github.tomboyo.lily.ast.type;

public sealed interface Type permits StandardType, AstClass, ClassRef, UnsupportedType {}
