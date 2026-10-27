/*
 * My Project
 */

package interpreter.fake;

import environment.Environment;
import evaluator.ExpressionEvaluator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import node.expression.ExpressionNode;
import node.expression.literal.DataType;
import result.Result;

/** Expression evaluator that always answers the same result and records target types. */
public final class StubExpressionEvaluator implements ExpressionEvaluator {
    private final Result<Object> answer;
    private final List<DataType> targetTypes = new ArrayList<>();

    public StubExpressionEvaluator(Result<Object> answer) {
        this.answer = answer;
    }

    @Override
    public Result<Object> evaluate(ExpressionNode expr, Environment env) {
        return answer;
    }

    @Override
    public Result<Object> evaluate(ExpressionNode expr, Environment env, DataType targetType) {
        targetTypes.add(targetType);
        return answer;
    }

    public List<DataType> targetTypes() {
        return Collections.unmodifiableList(new ArrayList<>(targetTypes));
    }
}
