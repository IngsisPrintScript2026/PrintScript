package node.keyword;

import node.Node;
import node.expression.ExpressionNode;

import java.util.ArrayList;
import java.util.List;

public record IfKeywordNode(
        ExpressionNode condition,
        List<Node> thenBody,
        List<Node> elseBody,
        Integer line,
        Integer column) implements Node {

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
}
