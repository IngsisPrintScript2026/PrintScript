/*
 * My Project
 */

package semantic.handler;

import node.expression.literal.DataType;
import node.keyword.WhileKeywordNode;
import result.CorrectResult;
import result.IncorrectResult;
import result.Result;
import semantic.environment.SemanticEnvironment;
import semantic.evaluator.ExpressionTypeInference;

public class WhileNodeSemanticHandler implements SemanticNodeHandler<WhileKeywordNode> {
    private final ExpressionTypeInference typeInference;

    public WhileNodeSemanticHandler(ExpressionTypeInference typeInference) {
        this.typeInference = typeInference;
    }

    @Override
    public Class<WhileKeywordNode> nodeType() {
        return WhileKeywordNode.class;
    }

    @Override
    public Result<SemanticEnvironment> check(WhileKeywordNode whileNode, SemanticEnvironment env) {
        return switch (typeInference.inferType(whileNode.condition(), env)) {
            case IncorrectResult<DataType>(String error) -> Result.failure(error);
            case CorrectResult<DataType>(DataType type) ->
                    type == DataType.BOOLEAN
                            ? Result.success(env)
                            : Result.failure(
                                    "Condition of 'while' at line "
                                            + whileNode.line()
                                            + " must be boolean");
        };
    }
}
