/*
 * My Project
 */

package evaluator;

import environment.Environment;
import node.expression.ExpressionNode;
import node.expression.literal.DataType;
import result.Result;

public interface ExpressionEvaluator {
    Result<Object> evaluate(ExpressionNode expr, Environment env);

    Result<Object> evaluate(ExpressionNode expr, Environment env, DataType targetType);
}
