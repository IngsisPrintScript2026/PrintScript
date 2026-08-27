/*
 * My Project
 */

package node.expression.function;

import java.util.ArrayList;
import java.util.List;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.visitor.NodeVisitor;

public record CallFunctionNode(
        IdentifierNode identifierNode,
        List<ExpressionNode> argumentNodes,
        Integer line,
        Integer column)
        implements ExpressionNode {

    public CallFunctionNode {
        argumentNodes = List.copyOf(argumentNodes);
    }

    @Override
    public List<ExpressionNode> children() {
        List<ExpressionNode> children = new ArrayList<>();
        children.add(identifierNode);
        children.addAll(argumentNodes);
        return children;
    }

    @Override
    public String symbol() {
        return identifierNode.name();
    }

    @Override
    public <R, C> R accept(NodeVisitor<R, C> visitor, C context) {
        return visitor.visit(this, context);
    }
}
