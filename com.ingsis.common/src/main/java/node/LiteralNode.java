/*
 * My Project
 */

package node;

import result.Result;

public sealed interface LiteralNode<T> extends ExpressionNode
        permits NumberLiteralNode, StringLiteralNode, BooleanLiteralNode {
    Result<T> value();
}
