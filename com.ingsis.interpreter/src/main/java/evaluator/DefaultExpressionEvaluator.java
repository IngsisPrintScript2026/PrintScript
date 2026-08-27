/*
 * My Project
 */

package evaluator;

import builtin.BuiltInFunction;
import builtin.FunctionRegistry;
import builtin.ReadEnvFunction;
import builtin.ReadInputFunction;
import environment.Environment;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.function.CallFunctionNode;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.DataType;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.expression.nullObject.NilExpressionNode;
import node.expression.operator.OperatorNode;
import result.CorrectResult;
import result.Result;

public class DefaultExpressionEvaluator implements ExpressionEvaluator {
    private final FunctionRegistry functionRegistry;
    private final Consumer<String> outputEmitter;

    public DefaultExpressionEvaluator(
            FunctionRegistry functionRegistry, Consumer<String> outputEmitter) {
        this.functionRegistry = functionRegistry;
        this.outputEmitter = outputEmitter;
    }

    public DefaultExpressionEvaluator() {
        this(null, null);
    }

    @Override
    public Result<Object> evaluate(ExpressionNode expr, Environment env) {
        return evaluate(expr, env, DataType.STRING);
    }

    @Override
    public Result<Object> evaluate(ExpressionNode expr, Environment env, DataType targetType) {
        return switch (expr) {
            case NumberLiteralNode num -> Result.success(num.rawValue());
            case StringLiteralNode str -> Result.success(str.rawValue());
            case BooleanLiteralNode bool -> Result.success(bool.rawValue());
            case NilExpressionNode nil -> Result.success(null);
            case IdentifierNode id -> evaluateIdentifier(id, env);
            case OperatorNode op -> evaluateOperator(op, env);
            case CallFunctionNode call -> evaluateCallFunction(call, env, targetType);
            default -> Result.failure("Unsupported expression node: " + expr);
        };
    }

    private Result<Object> evaluateIdentifier(IdentifierNode id, Environment env) {
        try {
            return Result.success(env.get(id.name()).value());
        } catch (Exception e) {
            return Result.failure("Variable error: " + e.getMessage());
        }
    }

    private Result<Object> evaluateCallFunction(
            CallFunctionNode call, Environment env, DataType targetType) {
        String fnName = call.identifierNode().name();
        List<Object> args = new ArrayList<>();
        for (ExpressionNode argNode : call.argumentNodes()) {
            Result<Object> argRes = evaluate(argNode, env);
            if (!argRes.isCorrect()) return argRes;
            args.add(((CorrectResult<Object>) argRes).value());
        }

        if (functionRegistry != null && functionRegistry.contains(fnName)) {
            BuiltInFunction fn = functionRegistry.get(fnName);
            try {
                if (fn instanceof ReadInputFunction readInput) {
                    return Result.success(readInput.evaluate(args, targetType, outputEmitter));
                } else if (fn instanceof ReadEnvFunction readEnv) {
                    return Result.success(readEnv.evaluate(args, targetType));
                }
            } catch (Exception e) {
                return Result.failure("Function execution error: " + e.getMessage());
            }
        }
        return Result.failure("Unsupported function call: " + fnName);
    }

    private Result<Object> evaluateOperator(OperatorNode op, Environment env) {
        Result<Object> leftRes = evaluate(op.left(), env);
        if (!leftRes.isCorrect()) return leftRes;
        Result<Object> rightRes = evaluate(op.right(), env);
        if (!rightRes.isCorrect()) return rightRes;

        Object left = ((CorrectResult<Object>) leftRes).value();
        Object right = ((CorrectResult<Object>) rightRes).value();

        try {
            return switch (op.operatorType()) {
                case PLUS -> {
                    if (left instanceof String || right instanceof String) {
                        yield Result.success(String.valueOf(left) + String.valueOf(right));
                    }
                    yield Result.success(((BigDecimal) left).add((BigDecimal) right));
                }
                case MINUS -> Result.success(((BigDecimal) left).subtract((BigDecimal) right));
                case STAR -> Result.success(((BigDecimal) left).multiply((BigDecimal) right));
                case SLASH -> Result.success(((BigDecimal) left).divide((BigDecimal) right));
                case ASSIGNATION -> Result.success(right);
            };
        } catch (Exception e) {
            return Result.failure("Operator evaluation error: " + e.getMessage());
        }
    }
}
