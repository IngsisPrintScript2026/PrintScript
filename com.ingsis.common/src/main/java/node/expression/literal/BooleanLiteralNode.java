package node.expression.literal;

import node.expression.ExpressionNode;
import node.expression.LiteralNode;
import result.Result;

import java.util.Collections;
import java.util.List;

public final record BooleanLiteralNode(Boolean rawValue, Integer line, Integer column)
        implements LiteralNode<Boolean> {

    @Override
    public Result<Boolean> value() {
        return Result.success(rawValue);
    }

    @Override
    public List<ExpressionNode> children() {
        return Collections.emptyList();
    }

    @Override
    public String symbol() {
        return String.valueOf(rawValue);
    }

}