/*
 * My Project
 */

package node;

import java.util.ArrayList;
import java.util.List;

public record IfKeywordNode(
        ExpressionNode condition,
        List<StatementNode> thenBody,
        List<StatementNode> elseBody,
        Integer line,
        Integer column)
        implements StatementNode {

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
