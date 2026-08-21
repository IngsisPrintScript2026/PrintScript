package semantic.handler;

import node.expression.literal.DataType;
import node.expression.nullObject.NilExpressionNode;
import node.keyword.DeclarationKeywordNode;
import result.CorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import semantic.evaluator.ExpressionTypeInference;

public class DeclarationNodeSemanticHandler implements SemanticNodeHandler<DeclarationKeywordNode> {
    private final ExpressionTypeInference typeInferencer = new ExpressionTypeInference();

    @Override
    public Class<DeclarationKeywordNode> nodeType() {
        return DeclarationKeywordNode.class;
    }

    @Override
    public Result<SemanticEnvironment> check(DeclarationKeywordNode decl, SemanticEnvironment env) {
        String varName = decl.identifierNode().name();
        if (env.lookup(varName).isPresent()) {
            return Result.failure("Variable '" + varName + "' is already declared at line " + decl.line());
        }

        boolean isInitialized = !(decl.expressionNode() instanceof NilExpressionNode);
        if (!isInitialized) {
            return Result.success(env.define(varName, null, decl.isMutable(), false));
        }

        Result<DataType> exprTypeRes = typeInferencer.inferType(decl.expressionNode(), env);
        if (!exprTypeRes.isCorrect()) {
            return Result.failure(((result.IncorrectResult<DataType>) exprTypeRes).error());
        }

        DataType inferredType = ((CorrectResult<DataType>) exprTypeRes).value();
        return Result.success(env.define(varName, inferredType, decl.isMutable(), true));
    }
}
