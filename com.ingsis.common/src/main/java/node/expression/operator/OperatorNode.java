package node.expression.operator;

import node.expression.ExpressionNode;

import java.util.List;

public record OperatorNode(
        OperatorType operatorType,
        List<ExpressionNode> children,
        Integer line,
        Integer column) implements ExpressionNode {

    @Override
    public String symbol() {
        return operatorType.symbol();
    }
}