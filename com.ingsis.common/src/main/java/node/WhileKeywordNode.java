/*
 * My Project
 */

package node;

import java.util.ArrayList;
import java.util.List;

public record WhileKeywordNode(
        ExpressionNode condition, List<StatementNode> body, Integer line, Integer column)
        implements StatementNode {

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
}
