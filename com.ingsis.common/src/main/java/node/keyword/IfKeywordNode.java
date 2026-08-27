/*
 * My Project
 */

package node.keyword;

import java.util.ArrayList;
import java.util.List;
import node.Node;
import node.expression.ExpressionNode;
import node.visitor.NodeVisitor;

public record IfKeywordNode(
        ExpressionNode condition,
        List<Node> thenBody,
        List<Node> elseBody,
        Integer line,
        Integer column)
        implements Node {

    public IfKeywordNode {
        thenBody = List.copyOf(thenBody);
        elseBody = List.copyOf(elseBody);
    }

    @Override
    public String symbol() {
        return "if";
    }

    @Override
    public List<Node> children() {
        List<Node> children = new ArrayList<>();
        children.add(condition);
        children.addAll(thenBody);
        children.addAll(elseBody);
        return children;
    }

    @Override
    public <R, C> R accept(NodeVisitor<R, C> visitor, C context) {
        return visitor.visit(this, context);
    }
}
