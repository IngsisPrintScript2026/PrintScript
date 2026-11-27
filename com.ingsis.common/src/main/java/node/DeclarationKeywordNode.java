/*
 * My Project
 */

package node;

import java.util.List;
import node.expression.literal.DataType;
import node.keyword.declaration.DeclarationType;

public record DeclarationKeywordNode(
        DeclarationType declarationType,
        IdentifierNode identifierNode,
        ExpressionNode expressionNode,
        DataType declaredType,
        Integer line,
        Integer column)
        implements StatementNode {

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
}
