/*
 * My Project
 */

package node.expression;

import java.util.List;
import node.Node;

public interface ExpressionNode extends Node {

    List<ExpressionNode> children();

    String symbol();
}
