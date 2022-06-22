package com.github.tomboyo.lily.compiler.ast;

/** An operation, such as "createNewBlogPost" corresponding to an OAS operation. */
public record AstOperation(String packageName, String name) implements Ast, Fqn {}
