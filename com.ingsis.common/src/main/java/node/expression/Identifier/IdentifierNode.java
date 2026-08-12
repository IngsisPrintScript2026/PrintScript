package node.expression.Identifier;

import node.expression.ExpressionNode;

import java.util.Collections;
import java.util.List;

public record IdentifierNode(String name, Integer line, Integer column)
        implements ExpressionNode {

    @Override
    public List<ExpressionNode> children() {
        return Collections.emptyList();
    }

    @Override
    public String symbol() {
        return name;
    }


}