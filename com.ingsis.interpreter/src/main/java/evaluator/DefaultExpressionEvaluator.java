package evaluator;

import environment.Environment;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.literal.BooleanLiteralNode;
import node.expression.literal.NumberLiteralNode;
import node.expression.literal.StringLiteralNode;
import node.expression.nullObject.NilExpressionNode;
import node.expression.operator.OperatorNode;

import java.math.BigDecimal;

public class DefaultExpressionEvaluator implements ExpressionEvaluator {

    @Override
    public Object evaluate(ExpressionNode expr, Environment env) {
        return switch (expr) {
            case NumberLiteralNode num -> num.rawValue();
            case StringLiteralNode str -> str.rawValue();
            case BooleanLiteralNode bool -> bool.rawValue();
            case NilExpressionNode nil -> null;
            case IdentifierNode id -> env.get(id.name()).value();
            case OperatorNode op -> evaluateOperator(op, env);
            default -> throw new IllegalArgumentException("Unsupported expression node: " + expr);
        };
    }

    private Object evaluateOperator(OperatorNode op, Environment env) {
        Object left = evaluate(op.left(), env);
        Object right = evaluate(op.right(), env);

        return switch (op.operatorType()) {
            case PLUS -> {
                if (left instanceof String || right instanceof String) {
                    yield String.valueOf(left) + String.valueOf(right);
                }
                yield ((BigDecimal) left).add((BigDecimal) right);
            }
            case MINUS -> ((BigDecimal) left).subtract((BigDecimal) right);
            case STAR -> ((BigDecimal) left).multiply((BigDecimal) right);
            case SLASH -> ((BigDecimal) left).divide((BigDecimal) right);
            case ASSIGNATION -> right;
        };
    }
}
