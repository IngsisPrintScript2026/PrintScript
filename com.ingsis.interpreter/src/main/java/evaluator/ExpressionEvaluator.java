package evaluator;

import environment.Environment;
import node.expression.ExpressionNode;

public interface ExpressionEvaluator {
    Object evaluate(ExpressionNode expr, Environment env);
}
