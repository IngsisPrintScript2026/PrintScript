package semantic.handler;

import node.expression.ExpressionNode;
import node.expression.function.CallFunctionNode;
import result.Result;
import semantic.environment.SemanticEnvironment;
import semantic.evaluator.ExpressionTypeInference;

public class CallFunctionNodeSemanticHandler implements SemanticNodeHandler<CallFunctionNode> {
    private final ExpressionTypeInference typeInference = new ExpressionTypeInference();

    @Override
    public Class<CallFunctionNode> nodeType() {
        return CallFunctionNode.class;
    }

    @Override
    public Result<SemanticEnvironment> check(CallFunctionNode call, SemanticEnvironment env) {
        for (ExpressionNode arg : call.argumentNodes()) {
            Result<?> argRes = typeInference.inferType(arg, env);
            if (!argRes.isCorrect()) {
                return Result.failure(((result.IncorrectResult<?>) argRes).error());
            }
        }
        return Result.success(env);
    }
}
