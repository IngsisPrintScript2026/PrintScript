/*
 * My Project
 */

package node.keyword;

import java.util.List;
import node.Node;
import node.expression.ExpressionNode;
import node.expression.Identifier.IdentifierNode;
import node.expression.literal.DataType;
import node.keyword.declaration.DeclarationType;
import node.visitor.NodeVisitor;

public record DeclarationKeywordNode(
        DeclarationType declarationType,
        IdentifierNode identifierNode,
        ExpressionNode expressionNode,
        DataType declaredType,
        Integer line,
        Integer column)
        implements Node {

    public DataType dataType() {
        return declaredType;
    }

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

    @Override
    public <R, C> R accept(NodeVisitor<R, C> visitor, C context) {
        return visitor.visit(this, context);
    }
}
