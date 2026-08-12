package node.keyword;

import node.Node;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;

import java.util.List;

public record AssignNode(
        IdentifierNode identifierNode,
        ExpressionNode expressionNode,
        Integer line,
        Integer column) implements Node {

    @Override
    public String symbol() {
        return "=";
    }

    @Override
    public List<Node> children() {
        return List.of(identifierNode, expressionNode);
    }

}
