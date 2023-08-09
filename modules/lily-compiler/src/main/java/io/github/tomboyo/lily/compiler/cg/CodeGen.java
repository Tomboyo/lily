package io.github.tomboyo.lily.compiler.cg;

import static io.github.tomboyo.lily.compiler.cg.AstApiCodeGen.renderAstAPi;
import static io.github.tomboyo.lily.compiler.cg.AstClassAliasCodeGen.renderAstClassAlias;
import static io.github.tomboyo.lily.compiler.cg.AstClassCodeGen.renderClass;
import static io.github.tomboyo.lily.compiler.cg.AstHeadersCodeGen.renderAstHeaders;
import static io.github.tomboyo.lily.compiler.cg.AstInterfaceCodeGen.renderAstInterface;
import static io.github.tomboyo.lily.compiler.cg.AstOperationCodeGen.renderAstOperation;
import static io.github.tomboyo.lily.compiler.cg.AstResponseCodeGen.renderAstResponse;
import static io.github.tomboyo.lily.compiler.cg.AstResponseSumCodeGen.renderAstResponseSum;
import static io.github.tomboyo.lily.compiler.cg.AstTaggedOperationCodeGen.renderAstTaggedOperations;

import io.github.tomboyo.lily.compiler.ast.Ast;
import io.github.tomboyo.lily.compiler.ast.AstApi;
import io.github.tomboyo.lily.compiler.ast.AstClass;
import io.github.tomboyo.lily.compiler.ast.AstClassAlias;
import io.github.tomboyo.lily.compiler.ast.AstHeaders;
import io.github.tomboyo.lily.compiler.ast.AstInterface;
import io.github.tomboyo.lily.compiler.ast.AstOperation;
import io.github.tomboyo.lily.compiler.ast.AstResponse;
import io.github.tomboyo.lily.compiler.ast.AstResponseSum;
import io.github.tomboyo.lily.compiler.ast.AstTaggedOperations;

/** Generates java source code from AST */
public class CodeGen {
  public static Source renderAst(Ast ast) {
    if (ast instanceof AstApi astApi) {
      return renderAstAPi(astApi);
    } else if (ast instanceof AstClass astClass) {
      return renderClass(astClass);
    } else if (ast instanceof AstClassAlias astClassAlias) {
      return renderAstClassAlias(astClassAlias);
    } else if (ast instanceof AstHeaders astHeaders) {
      return renderAstHeaders(astHeaders);
    } else if (ast instanceof AstInterface astInterface) {
      return renderAstInterface(astInterface);
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
