/*
 * My Project
 */

package semantic.evaluator;

import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import syntactic.version.VersionStrategyRegistry;
import version.Version;

public class ExpressionTypeInference {
    private final Version version;
    private final Set<String> builtInFunctions;

    public ExpressionTypeInference(Version version) {
        this.version = version;
        this.builtInFunctions =
                new VersionStrategyRegistry().getStrategy(version).builtInFunctions();
    }

    public ExpressionTypeInference() {
        this(Version.V_1_1);
    }

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
        if (!leftTypeRes.isCorrect()) {
            return leftTypeRes;
        }
        Result<DataType> rightTypeRes = inferType(op.right(), env);
        if (!rightTypeRes.isCorrect()) {
            return rightTypeRes;
        }
        DataType left = ((result.CorrectResult<DataType>) leftTypeRes).value();
        DataType right = ((result.CorrectResult<DataType>) rightTypeRes).value();
        return resolveOperatorResultType(op, left, right);
    }

    private Result<DataType> resolveOperatorResultType(
            OperatorNode op, DataType left, DataType right) {
        return switch (op.operatorType()) {
            case PLUS -> resolvePlusType(op, left, right);
            case MINUS, STAR, SLASH ->
                    (left == DataType.NUMBER && right == DataType.NUMBER)
                            ? Result.success(DataType.NUMBER)
                            : Result.failure(
                                    "Numeric operator requires number operands at line "
                                            + op.line());
            case ASSIGNATION -> Result.success(right);
        };
    }

    private Result<DataType> resolvePlusType(OperatorNode op, DataType left, DataType right) {
        if (left == DataType.STRING || right == DataType.STRING) {
            return Result.success(DataType.STRING);
        }
        if (left == DataType.NUMBER && right == DataType.NUMBER) {
            return Result.success(DataType.NUMBER);
        }
        return Result.failure("Incompatible operands for '+' operator at line " + op.line());
    }

    private Result<DataType> inferFunctionType(CallFunctionNode call, SemanticEnvironment env) {
        String fnName = call.identifierNode().name();
        if (!builtInFunctions.contains(fnName)) {
            return Result.failure(
                    "Function '"
                            + fnName
                            + "' is not supported in PrintScript "
                            + (version == Version.V_1_0 ? "1.0" : "1.1"));
        }
        Result<DataType> argsRes = checkArguments(call.argumentNodes(), env);
        if (!argsRes.isCorrect()) {
            return argsRes;
        }
        // readInput/readEnv adapt to the type of the variable that receives them (null = any)
        boolean adaptsToTarget = "readInput".equals(fnName) || "readEnv".equals(fnName);
        return Result.success(adaptsToTarget ? null : DataType.STRING);
    }

    private Result<DataType> checkArguments(List<ExpressionNode> args, SemanticEnvironment env) {
        for (ExpressionNode arg : args) {
            Result<DataType> argRes = inferType(arg, env);
            if (!argRes.isCorrect()) {
                return argRes;
            }
        }
        return Result.success(null);
    }
}
