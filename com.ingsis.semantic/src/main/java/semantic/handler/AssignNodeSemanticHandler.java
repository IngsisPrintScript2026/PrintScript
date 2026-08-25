package semantic.handler;

import node.expression.literal.DataType;
import node.keyword.AssignNode;
import result.CorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import semantic.evaluator.ExpressionTypeInference;

import java.util.Optional;

public class AssignNodeSemanticHandler implements SemanticNodeHandler<AssignNode> {
    private final ExpressionTypeInference typeInferencer = new ExpressionTypeInference();

    @Override
    public Class<AssignNode> nodeType() {
        return AssignNode.class;
    }

    @Override
    public Result<SemanticEnvironment> check(AssignNode assign, SemanticEnvironment env) {
        String varName = assign.identifierNode().name();
        Optional<SemanticEnvironment.VariableSymbol> symbolOpt = env.lookup(varName);

        if (symbolOpt.isEmpty()) {
            return Result.failure("Cannot assign to undeclared variable '" + varName + "' at line " + assign.line());
        }

        SemanticEnvironment.VariableSymbol symbol = symbolOpt.get();
        if (!symbol.isMutable() && symbol.isInitialized()) {
            return Result.failure("Cannot reassign constant variable '" + varName + "' at line " + assign.line());
        }

        Result<DataType> exprTypeRes = typeInferencer.inferType(assign.expressionNode(), env);
        if (!exprTypeRes.isCorrect()) {
            return Result.failure(((result.IncorrectResult<DataType>) exprTypeRes).error());
        }

        DataType exprType = ((CorrectResult<DataType>) exprTypeRes).value();
        if (symbol.type() != null && symbol.type() != exprType) {
            return Result.failure("Type mismatch in assignment to variable '" + varName + "' at line " + assign.line());
        }

        return Result.success(env.define(varName, exprType, symbol.isMutable(), true));
    }
}
