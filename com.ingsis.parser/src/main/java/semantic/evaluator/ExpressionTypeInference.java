/*
 * My Project
 */

package semantic.evaluator;

import java.util.Optional;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.DataType;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.expression.nullObject.NilExpressionNode;
import node.expression.operator.OperatorNode;
import result.Result;
import semantic.environment.SemanticEnvironment;

public class ExpressionTypeInference {

    public Result<DataType> inferType(ExpressionNode expr, SemanticEnvironment env) {
        return switch (expr) {
            case NumberLiteralNode num -> Result.success(DataType.NUMBER);
            case StringLiteralNode str -> Result.success(DataType.STRING);
            case BooleanLiteralNode bool -> Result.success(DataType.BOOLEAN);
            case NilExpressionNode nil -> Result.failure("Cannot infer type from Nil expression");
            case IdentifierNode id -> inferIdentifierType(id, env);
            case OperatorNode op -> inferOperatorType(op, env);
            case CallFunctionNode call -> inferFunctionType(call, env);
            default -> Result.failure("Unsupported expression node for type inference: " + expr);
        };
    }

    private Result<DataType> inferIdentifierType(IdentifierNode id, SemanticEnvironment env) {
        Optional<SemanticEnvironment.VariableSymbol> symbolOpt = env.lookup(id.name());
        if (symbolOpt.isEmpty()) {
            return Result.failure(
                    "Variable '" + id.name() + "' is not declared (line " + id.line() + ")");
        }
        return Result.success(symbolOpt.get().type());
    }

    private Result<DataType> inferOperatorType(OperatorNode op, SemanticEnvironment env) {
        Result<DataType> leftTypeRes = inferType(op.left(), env);
        if (!leftTypeRes.isCorrect()) return leftTypeRes;

        Result<DataType> rightTypeRes = inferType(op.right(), env);
        if (!rightTypeRes.isCorrect()) return rightTypeRes;

        DataType left = ((result.CorrectResult<DataType>) leftTypeRes).value();
        DataType right = ((result.CorrectResult<DataType>) rightTypeRes).value();

        return switch (op.operatorType()) {
            case PLUS ->
                    (left == DataType.STRING || right == DataType.STRING)
                            ? Result.success(DataType.STRING)
                            : (left == DataType.NUMBER && right == DataType.NUMBER)
                                    ? Result.success(DataType.NUMBER)
                                    : Result.failure(
                                            "Incompatible operands for '+' operator at line "
                                                    + op.line());
            case MINUS, STAR, SLASH ->
                    (left == DataType.NUMBER && right == DataType.NUMBER)
                            ? Result.success(DataType.NUMBER)
                            : Result.failure(
                                    "Numeric operator requires number operands at line "
                                            + op.line());
            case ASSIGNATION -> Result.success(right);
        };
    }

    private Result<DataType> inferFunctionType(CallFunctionNode call, SemanticEnvironment env) {
        String fnName = call.identifierNode().name();
        if ("readInput".equalsIgnoreCase(fnName) || "readEnv".equalsIgnoreCase(fnName)) {
            return Result.success(null);
        }
        return Result.success(DataType.STRING);
    }
}
