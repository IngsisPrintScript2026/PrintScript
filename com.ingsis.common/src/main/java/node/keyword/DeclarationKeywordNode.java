package node.keyword;

import node.Node;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.keyword.declaration.DeclarationType;

import java.sql.Types;
import java.util.Collections;
import java.util.List;

public record DeclarationKeywordNode(
        DeclarationType declarationType,
        IdentifierNode identifierNode,
        ExpressionNode expressionNode,
        Types declaredType,
        Integer line,
        Integer column) implements Node {

    @Override
    public String symbol() {
        return declarationType.keyword();
    }

    @Override
    public List<Node> children() {
        return List.of(identifierNode, expressionNode);
    }

    public boolean isMutable() {
        return declarationType.isMutable();
    }
}


