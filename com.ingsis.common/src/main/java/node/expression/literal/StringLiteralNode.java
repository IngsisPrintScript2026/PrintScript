package node.expression.literal;

import node.expression.ExpressionNode;
import node.expression.LiteralNode;
import result.Result;

import java.util.Collections;
import java.util.List;

public final record StringLiteralNode(String rawValue, Integer line, Integer column)
        implements LiteralNode<String> {

    @Override
    public Result<String> value() {
        return Result.success(rawValue);
    }

    @Override
    public List<ExpressionNode> children() {
        return Collections.emptyList();
    }

    @Override
    public String symbol() {
        return rawValue;
    }
}

