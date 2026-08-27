/*
 * My Project
 */

package semantic.handler;

import node.expression.literal.DataType;
import node.keyword.IfKeywordNode;
import result.CorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import semantic.evaluator.ExpressionTypeInference;

public class IfNodeSemanticHandler implements SemanticNodeHandler<IfKeywordNode> {
    private final ExpressionTypeInference typeInference = new ExpressionTypeInference();

    @Override
    public Class<IfKeywordNode> nodeType() {
        return IfKeywordNode.class;
    }

    @Override
    public Result<SemanticEnvironment> check(IfKeywordNode ifNode, SemanticEnvironment env) {
        Result<DataType> condTypeRes = typeInference.inferType(ifNode.condition(), env);
        if (!condTypeRes.isCorrect()) {
            return Result.failure(((result.IncorrectResult<DataType>) condTypeRes).error());
        }

        DataType condType = ((CorrectResult<DataType>) condTypeRes).value();
        if (condType != DataType.BOOLEAN) {
            return Result.failure(
                    "Condition of 'if' at line " + ifNode.line() + " must be boolean");
        }

        SemanticEnvironment thenEnv = new SemanticEnvironment(env);
        return Result.success(env);
    }
}
