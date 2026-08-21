package node.keyword;

import node.Node;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.visitor.NodeVisitor;

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

    @Override
    public <R, C> R accept(NodeVisitor<R, C> visitor, C context) {
        return visitor.visit(this, context);
    }

}
