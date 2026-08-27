/*
 * My Project
 */

package node.expression.operator;

import java.util.List;
import node.expression.ExpressionNode;

public record OperatorNode(
        OperatorType operatorType,
        ExpressionNode left,
        ExpressionNode right,
        Integer line,
        Integer column)
        implements ExpressionNode {

    @Override
    public String symbol() {
        return operatorType.symbol();
    }

    @Override
    public List<ExpressionNode> children() {
        return List.of(left, right);
    }
}
