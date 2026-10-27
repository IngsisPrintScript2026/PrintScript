/*
 * My Project
 */

package semantic.handler;

import node.expression.function.CallFunctionNode;
import result.Result;
import semantic.environment.SemanticEnvironment;
import semantic.evaluator.ExpressionTypeInference;

public class CallFunctionNodeSemanticHandler implements SemanticNodeHandler<CallFunctionNode> {
    private final ExpressionTypeInference typeInference;

    public CallFunctionNodeSemanticHandler(ExpressionTypeInference typeInference) {
        this.typeInference = typeInference;
    }

    public CallFunctionNodeSemanticHandler() {
        this(new ExpressionTypeInference());
    }

    @Override
    public Class<CallFunctionNode> nodeType() {
        return CallFunctionNode.class;
    }

    @Override
    public Result<SemanticEnvironment> check(CallFunctionNode call, SemanticEnvironment env) {
        Result<?> callRes = typeInference.inferType(call, env);
        if (!callRes.isCorrect()) {
            return Result.failure(((result.IncorrectResult<?>) callRes).error());
        }
        return Result.success(env);
    }
}
