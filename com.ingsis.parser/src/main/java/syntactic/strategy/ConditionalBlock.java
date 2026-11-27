/*
 * My Project
 */

package syntactic.strategy;

import java.util.List;
import node.ExpressionNode;
import node.StatementNode;
import token.Token;

public record ConditionalBlock(
        Token ifToken, ExpressionNode condition, List<StatementNode> thenBody) {}
