/*
 * My Project
 */

package node;

import java.util.Collections;
import java.util.List;
import position.Position;
import position.SourceRange;
import result.Result;

public record BooleanLiteralNode(Boolean rawValue, Integer line, Integer column)
        implements LiteralNode<Boolean> {

    @Override
    public Result<Boolean> value() {
        return Result.success(rawValue);
    }

    @Override
    public SourceRange range() {
        return SourceRange.spanning(new Position(line, column), String.valueOf(rawValue));
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
