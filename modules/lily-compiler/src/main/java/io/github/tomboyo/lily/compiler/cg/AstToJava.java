package io.github.tomboyo.lily.compiler.cg;

import static io.github.tomboyo.lily.compiler.cg.AstApiToJavaSource.renderAstAPi;
import static io.github.tomboyo.lily.compiler.cg.AstClassAliasToJavaSource.renderAstClassAlias;
import static io.github.tomboyo.lily.compiler.cg.AstClassToJavaSource.renderClass;
import static io.github.tomboyo.lily.compiler.cg.AstHeadersCg.renderAstHeaders;
import static io.github.tomboyo.lily.compiler.cg.AstOperationToJavaSource.renderAstOperation;
import static io.github.tomboyo.lily.compiler.cg.AstResponseCg.renderAstResponse;
import static io.github.tomboyo.lily.compiler.cg.AstResponseSumToJavaSource.renderAstResponseSum;
import static io.github.tomboyo.lily.compiler.cg.AstTaggedOperationToJavaSource.renderAstTaggedOperations;

import io.github.tomboyo.lily.compiler.ast.*;

public class AstToJava {
  public static Source renderAst(Ast ast) {
    if (ast instanceof AstApi astApi) {
      return renderAstAPi(astApi);
    } else if (ast instanceof AstClass astClass) {
      return renderClass(astClass);
    } else if (ast instanceof AstClassAlias astClassAlias) {
      return renderAstClassAlias(astClassAlias);
    } else if (ast instanceof AstHeaders astHeaders) {
      return renderAstHeaders(astHeaders);
    } else if (ast instanceof AstOperation astOperation) {
      return renderAstOperation(astOperation);
    } else if (ast instanceof AstResponseSum astResponseSum) {
      return renderAstResponseSum(astResponseSum);
    } else if (ast instanceof AstResponse astResponse) {
      return renderAstResponse(astResponse);
    } else if (ast instanceof AstTaggedOperations astTaggedOperations) {
      return renderAstTaggedOperations(astTaggedOperations);
    } else {
      throw new IllegalArgumentException("Unsupported AST: " + ast);
    }
  }
}
