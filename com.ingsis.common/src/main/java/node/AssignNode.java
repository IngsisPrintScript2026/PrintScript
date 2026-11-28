/*
 * My Project
 */

package node;

import java.util.List;

public record AssignNode(
        IdentifierNode identifierNode, ExpressionNode expressionNode, Integer line, Integer column)
        implements StatementNode {

    @Override
    public String symbol() {
        return "=";
    }

    @Override
    public List<Node> children() {
        return List.of(identifierNode, expressionNode);
    }
}
