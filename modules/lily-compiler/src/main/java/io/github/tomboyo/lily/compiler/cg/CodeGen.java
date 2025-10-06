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
    return switch (ast) {
      case AstApi astApi -> renderAstAPi(astApi);
      case AstClass astClass -> renderClass(astClass);
      case AstClassAlias astClassAlias -> renderAstClassAlias(astClassAlias);
      // TODO: rendered headers are not currently used.
      case AstHeaders astHeaders -> renderAstHeaders(astHeaders);
      case AstInterface astInterface -> renderAstInterface(astInterface);
      case AstOperation astOperation -> renderAstOperation(astOperation);
      case AstResponseSum astResponseSum -> renderAstResponseSum(astResponseSum);
      case AstResponse astResponse -> renderAstResponse(astResponse);
      case AstTaggedOperations astTaggedOperations ->
          renderAstTaggedOperations(astTaggedOperations);
    };
  }
}
