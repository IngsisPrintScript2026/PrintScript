/*
 * My Project
 */

package node.expression;

import result.Result;

public interface LiteralNode<T> extends ExpressionNode {
    Result<T> value();
}
