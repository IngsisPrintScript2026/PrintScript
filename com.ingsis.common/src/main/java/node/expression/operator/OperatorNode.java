package node.expression.operator;

import node.expression.ExpressionNode;

import java.util.List;

public record OperatorNode(
        OperatorType operatorType,
        ExpressionNode left,
        ExpressionNode right,
        Integer line,
        Integer column) implements ExpressionNode {

    @Override
    public String symbol() {
        return operatorType.symbol();
    }

    @Override
    public List<ExpressionNode> children() {
        return List.of(left, right);
    }

}