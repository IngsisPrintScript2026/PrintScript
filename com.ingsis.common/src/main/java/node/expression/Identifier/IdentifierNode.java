/*
 * My Project
 */

package node.expression.Identifier;

import java.util.Collections;
import java.util.List;
import node.expression.ExpressionNode;
import position.Position;
import position.SourceRange;

public record IdentifierNode(String name, Integer line, Integer column) implements ExpressionNode {

    @Override
    public SourceRange range() {
        return SourceRange.spanning(new Position(line, column), name);
    }

    @Override
    public List<ExpressionNode> children() {
        return Collections.emptyList();
    }

    @Override
    public String symbol() {
        return name;
    }
}
