/*
 * My Project
 */

package node;

import java.util.List;

public sealed interface ExpressionNode extends StatementNode
        permits LiteralNode, IdentifierNode, OperatorNode, CallFunctionNode, NilExpressionNode {

    @Override
    List<ExpressionNode> children();

    @Override
    String symbol();
}
