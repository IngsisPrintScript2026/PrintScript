/*
 * My Project
 */

package node.keyword;

import java.util.ArrayList;
import java.util.List;
import node.Node;
import node.expression.ExpressionNode;
import node.visitor.NodeVisitor;

public record WhileKeywordNode(
        ExpressionNode condition, List<Node> body, Integer line, Integer column) implements Node {

    public WhileKeywordNode {
        body = List.copyOf(body);
    }

    @Override
    public String symbol() {
        return "while";
    }

    @Override
    public List<Node> children() {
        List<Node> children = new ArrayList<>();
        children.add(condition);
        children.addAll(body);
        return children;
    }

    @Override
    public <R, C> R accept(NodeVisitor<R, C> visitor, C context) {
        return visitor.visit(this, context);
    }
}
