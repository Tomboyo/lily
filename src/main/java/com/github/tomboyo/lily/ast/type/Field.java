package com.github.tomboyo.lily.ast.type;

public record Field(AstReference astReference, String name) {
    public Field(AstReference astReference, String name) {
        this.astReference = astReference;
        // to fieldNameCase
        this.name = name.substring(0, 1).toLowerCase() + name.substring(1);
    }
}
