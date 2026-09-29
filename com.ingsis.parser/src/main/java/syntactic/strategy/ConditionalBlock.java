/*
 * My Project
 */

package syntactic.strategy;

import java.util.List;
import node.Node;
import node.expression.ExpressionNode;
import token.Token;

public record ConditionalBlock(Token ifToken, ExpressionNode condition, List<Node> thenBody) {}
